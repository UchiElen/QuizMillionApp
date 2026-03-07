package com.dam.quizmillionapp.activities;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.dam.quizmillionapp.R;
import com.dam.quizmillionapp.adapters.RoomsAdapter;
import com.dam.quizmillionapp.auth.UserSession;
import com.dam.quizmillionapp.models.RoomSummary;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class LobbyActivity extends AppCompatActivity {

    // Variables principales de la pantalla: Firestore, listener de salas,
    // adapter del RecyclerView y controles del formulario del lobby.
    private FirebaseFirestore firestore;
    private ListenerRegistration roomsListener;
    private RoomsAdapter roomsAdapter;
    private EditText edtRoomCode;
    private Button btnJoinByCode;
    private Button btnCreateRoom;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lobby);

        // Inicializar Firestore y conectar la Activity con los elementos visuales del layout.
        firestore = FirebaseFirestore.getInstance();

        edtRoomCode = findViewById(R.id.edtRoomCode);
        btnJoinByCode = findViewById(R.id.btnJoinByCode);
        btnCreateRoom = findViewById(R.id.btnCreateRoom);

        RecyclerView rvRooms = findViewById(R.id.rvRooms);
        rvRooms.setLayoutManager(new LinearLayoutManager(this));

        // Preparar el adapter del listado de salas y definir qué ocurre al pulsar una sala.
        roomsAdapter = new RoomsAdapter(room -> {
            joinRoomByRoomId(room.getRoomId());
        });

        rvRooms.setAdapter(roomsAdapter);

        // Asociar las acciones principales del lobby a sus botones.
        btnCreateRoom.setOnClickListener(view -> {
            createNewRoom("Room");
        });

        btnJoinByCode.setOnClickListener(view -> {
            String code = edtRoomCode.getText().toString().trim().toUpperCase();

            if (code.length() != 6) {
                showToast("El código de la sala debe tener 6 caracteres.");
                return;
            }

            joinRoomByCode(code);
        });

        // Iniciar el listener en tiempo real para mostrar las salas disponibles.
        startRoomsRealtimeListener();
    }

    private void showToast(String message) {
        // Mostrar mensajes breves al usuario para confirmar acciones o avisar de errores.
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private void startRoomsRealtimeListener() {

        // Escuchar en tiempo real la colección de salas que siguen abiertas y disponibles.
        roomsListener = firestore.collection("rooms")
                .whereEqualTo("status", "waiting")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .addSnapshotListener((queryResult, error) -> {

                    // Gestionar posibles errores del listener antes de procesar los datos.
                    if (error != null) {
                        showToast("Error: " + error.getMessage());
                        return;
                    }

                    if (queryResult == null) {
                        return;
                    }

                    // Recorrer los documentos recibidos y construir la lista de salas para el RecyclerView.
                    List<RoomSummary> roomList = new ArrayList<>();

                    for (DocumentSnapshot roomDocument : queryResult.getDocuments()) {

                        String roomId = roomDocument.getId();
                        String name = roomDocument.getString("name");
                        String code = roomDocument.getString("code");
                        String status = roomDocument.getString("status");

                        Long playersCount = roomDocument.getLong("playersCount");
                        Long maxPlayers = roomDocument.getLong("maxPlayers");

                        long safePlayersCount = playersCount == null ? 0 : playersCount;
                        long safeMaxPlayers = maxPlayers == null ? 4 : maxPlayers;

                        roomList.add(
                                new RoomSummary(
                                        roomId,
                                        name,
                                        code,
                                        status,
                                        safePlayersCount,
                                        safeMaxPlayers
                                )
                        );
                    }

                    // Actualizar el listado visual con las salas obtenidas desde Firestore.
                    roomsAdapter.updateRooms(roomList);
                });
    }

    private void createNewRoom(String roomName) {

        // Obtener la identidad del usuario actual antes de crear la sala.
        String uid = UserSession.getCurrentUid();
        String displayName = UserSession.getCurrentDisplayName();

        if (uid == null) {
            showToast("Usuario no disponible. Login obligatorio.");
            return;
        }

        // Preparar el documento de la nueva sala y añadir al creador como primer miembro.
        String code = generateRoomCode();
        DocumentReference roomDoc = firestore.collection("rooms").document();

        Map<String, Object> roomData = new HashMap<>();
        roomData.put("code", code);
        roomData.put("name", roomName);
        roomData.put("hostUid", uid);
        roomData.put("status", "waiting");
        roomData.put("maxPlayers", 4);
        roomData.put("playersCount", 1);
        roomData.put("createdAt", FieldValue.serverTimestamp());

        DocumentReference memberDoc = roomDoc.collection("members").document(uid);

        Map<String, Object> memberData = new HashMap<>();
        memberData.put("displayName", displayName);
        memberData.put("joinedAt", FieldValue.serverTimestamp());
        memberData.put("score", 0);

        // Guardar la sala y el primer miembro en un único batch para asegurar consistencia.
        WriteBatch batch = firestore.batch();
        batch.set(roomDoc, roomData);
        batch.set(memberDoc, memberData);

        batch.commit()
                .addOnSuccessListener(unused -> {
                    openRoom(roomDoc.getId());
                })
                .addOnFailureListener(e -> {
                    showToast("Create room failed: " + e.getMessage());
                });
    }

    private void openRoom(String roomId) {

        // Abrir la pantalla de espera y enviar el identificador de la sala seleccionada o creada.
        Intent intent = new Intent(this, WaitingActivity.class);
        intent.putExtra("roomId", roomId);
        startActivity(intent);
    }

    private void joinRoomByRoomId(String roomId) {

        // Obtener y validar la identidad del usuario actual antes de intentar entrar en la sala.
        String currentUid = UserSession.getCurrentUid();
        String currentDisplayName = UserSession.getCurrentDisplayName();

        if (currentUid == null) {
            showToast("Login requerido.");
            return;
        }

        // Crear referencias a la sala y al documento del jugador dentro de esa sala.
        DocumentReference roomRef = firestore.collection("rooms").document(roomId);
        DocumentReference memberRef = roomRef.collection("members").document(currentUid);

        // Ejecutar una transacción para validar aforo, evitar duplicados y registrar al jugador.
        firestore.runTransaction(transaction -> {
                    DocumentSnapshot roomDocument = transaction.get(roomRef);
                    DocumentSnapshot memberDocument = transaction.get(memberRef);

                    if (!roomDocument.exists()) {
                        throw new RuntimeException("Sala no encontrada.");
                    }

                    String status = roomDocument.getString("status");
                    Long playersCount = roomDocument.getLong("playersCount");
                    Long maxPlayers = roomDocument.getLong("maxPlayers");

                    long safePlayersCount = playersCount == null ? 0 : playersCount;
                    long safeMaxPlayers = maxPlayers == null ? 4 : maxPlayers;

                    if (!"waiting".equals(status)) {
                        throw new RuntimeException("Sala no disponible.");
                    }

                    if (memberDocument.exists()) {
                        return true;
                    }

                    if (safePlayersCount >= safeMaxPlayers) {
                        throw new RuntimeException("La sala está llena.");
                    }

                    Map<String, Object> memberData = new HashMap<>();
                    memberData.put("displayName", currentDisplayName);
                    memberData.put("joinedAt", FieldValue.serverTimestamp());
                    memberData.put("score", 0);

                    transaction.set(memberRef, memberData);
                    transaction.update(roomRef, "playersCount", safePlayersCount + 1);

                    return false;
                })

                // Si la entrada es correcta, abrir la sala de espera.
                .addOnSuccessListener(alreadyJoined -> {
                    Boolean wasAlreadyMember = (Boolean) alreadyJoined;

                    if (Boolean.TRUE.equals(wasAlreadyMember)) {
                        showToast("Ya estás en esta sala.");
                    }

                    Intent intent = new Intent(LobbyActivity.this, WaitingActivity.class);
                    intent.putExtra("roomId", roomId);
                    startActivity(intent);
                })

                // Si la entrada falla, informar al usuario del motivo.
                .addOnFailureListener(e -> {
                    showToast("Error: " + e.getMessage());
                });
    }

    private void joinRoomByCode(String roomCode) {

        // Buscar una sala a partir del código introducido por el usuario.
        firestore.collection("rooms")
                .whereEqualTo("code", roomCode)
                .limit(1)
                .get()

                // Si se encuentra una sala válida, reutilizar la lógica de entrada por roomId.
                .addOnSuccessListener(queryResult -> {
                    if (queryResult.isEmpty()) {
                        showToast("Código de sala no encontrado.");
                        return;
                    }

                    DocumentSnapshot roomDocument = queryResult.getDocuments().get(0);
                    String roomId = roomDocument.getId();

                    joinRoomByRoomId(roomId);
                })

                // Si la búsqueda falla, mostrar el error correspondiente.
                .addOnFailureListener(e -> {
                    showToast("Error searching room: " + e.getMessage());
                });
    }

    private String generateRoomCode() {

        // Generar un código aleatorio de 6 caracteres para identificar la sala.
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
        StringBuilder sb = new StringBuilder();
        Random random = new Random();

        for (int i = 0; i < 6; i++) {
            int index = random.nextInt(chars.length());
            sb.append(chars.charAt(index));
        }

        return sb.toString();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // Liberar el listener del lobby al cerrar la pantalla para evitar fugas y duplicados.
        if (roomsListener != null) {
            roomsListener.remove();
        }
    }
}