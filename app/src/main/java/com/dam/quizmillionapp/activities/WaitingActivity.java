package com.dam.quizmillionapp.activities;

import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.dam.quizmillionapp.R;
import com.dam.quizmillionapp.adapters.MembersAdapter;
import com.dam.quizmillionapp.auth.UserSession;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

public class WaitingActivity extends AppCompatActivity {

    // Variables principales de la Activity: conexión con Firestore, identificador de sala,
    // referencias a listeners y componentes visuales de la pantalla.
    private FirebaseFirestore firestore;
    private String roomId;
    private DocumentReference roomRef;
    private ListenerRegistration roomListener;
    private ListenerRegistration membersListener;
    private TextView txtRoomCode;
    private TextView txtRoomStatus;
    private Button btnStartGame;
    private Button btnLeaveRoom;
    private MembersAdapter membersAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_waiting);

        // Inicializar Firestore y recuperar el roomId recibido desde el Lobby.
        firestore = FirebaseFirestore.getInstance();
        roomId = getIntent().getStringExtra("roomId");

        // Comprobar que la pantalla ha recibido un roomId válido antes de continuar.
        if (roomId == null || roomId.trim().isEmpty()) {
            showToast("RoomId perdido");
            finish();
            return;
        }

        // Crear la referencia al documento de la sala en Firestore.
        roomRef = firestore.collection("rooms").document(roomId);

        // Conectar las variables Java con los elementos visuales del layout.
        txtRoomCode = findViewById(R.id.txtRoomCode);
        txtRoomStatus = findViewById(R.id.txtRoomStatus);
        btnStartGame = findViewById(R.id.btnStartGame);
        btnLeaveRoom = findViewById(R.id.btnLeaveRoom);

        // Preparar el RecyclerView que mostrará la lista de jugadores conectados.
        RecyclerView rvMembers = findViewById(R.id.rvMembers);
        rvMembers.setLayoutManager(new LinearLayoutManager(this));
        membersAdapter = new MembersAdapter();
        rvMembers.setAdapter(membersAdapter);

        // Asociar las acciones principales de la pantalla a sus botones.
        btnStartGame.setOnClickListener(view -> startGameIfHost());
        btnLeaveRoom.setOnClickListener(view -> leaveRoomAndExit());

        // Iniciar los listeners en tiempo real para escuchar cambios en la sala y en sus miembros.
        startRealtimeListeners();
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
    private void startRealtimeListeners() {

        // Escuchar en tiempo real los cambios del documento principal de la sala.
        roomListener = roomRef.addSnapshotListener((snapshot, error) -> {
            if (error != null) {
                showToast("Room listener error: " + error.getMessage());
                return;
            }

            if (snapshot == null || !snapshot.exists()) {
                showToast("Sala no encontrada");
                finish();
                return;
            }

            // Leer el estado actual de la sala y reflejarlo en la interfaz.
            String code = snapshot.getString("code");
            String status = snapshot.getString("status");
            String hostUid = snapshot.getString("hostUid");

            txtRoomCode.setText("Code: " + (code != null ? code : "------"));
            txtRoomStatus.setText("Status: " + (status != null ? status : "unknown"));

            // Determinar si el usuario actual es el anfitrión y si puede iniciar la partida.
            String myUid = UserSession.getCurrentUid();
            boolean isHost = myUid != null && myUid.equals(hostUid);
            boolean canStart = isHost && "waiting".equals(status);

            btnStartGame.setEnabled(canStart);

            // Detectar cuándo la sala pasa a estado de juego iniciado.
            if ("in_progress".equals(status)) {
                showToast("Juego iniciado! (Próxima ventana)");
            }
        });

        // Escuchar en tiempo real la subcolección de miembros para mantener actualizada la lista de jugadores.
        membersListener = roomRef.collection("members")
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null) {
                        showToast("Members listener error: " + error.getMessage());
                        return;
                    }

                    if (snapshot == null) {
                        return;
                    }

                    // Recorrer los miembros de la sala y construir la lista de nombres que verá el usuario.
                    List<String> names = new ArrayList<>();
                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        String name = doc.getString("displayName");
                        if (name == null || name.trim().isEmpty()) {
                            name = doc.getId();
                        }
                        names.add(name);
                    }

                    // Actualizar el RecyclerView con la lista de jugadores conectados.
                    membersAdapter.updateMembers(names);
                });
    }

    private void startGameIfHost() {

        // Obtener la identidad del usuario actual antes de comprobar si puede iniciar la sala.
        String myUid = UserSession.getCurrentUid();
        if (myUid == null) {
            showToast("Usuario no disponible");
            return;
        }

        // Verificar que la sala existe y que el usuario actual es realmente el anfitrión.
        roomRef.get().addOnSuccessListener(snapshot -> {
            if (!snapshot.exists()) {
                showToast("Sala no encontrada");
                return;
            }

            String hostUid = snapshot.getString("hostUid");
            if (hostUid == null || !hostUid.equals(myUid)) {
                showToast("Only host can start.");
                return;
            }

            // Cambiar el estado de la sala para que todos los jugadores detecten el inicio de la partida.
            Map<String, Object> updates = new HashMap<>();
            updates.put("status", "in_progress");
            updates.put("startedAt", FieldValue.serverTimestamp());

            roomRef.update(updates)
                    .addOnSuccessListener(unused -> showToast("Started!"))
                    .addOnFailureListener(e -> showToast("Start failed: " + e.getMessage()));
        }).addOnFailureListener(e -> {
            showToast("Start check failed: " + e.getMessage());
        });
    }

    private void leaveRoomAndExit() {

        // Obtener el usuario actual para eliminarlo correctamente de la sala.
        String uid = UserSession.getCurrentUid();
        if (uid == null) {
            finish();
            return;
        }

        DocumentReference memberRef = roomRef.collection("members").document(uid);

        // Ejecutar una transacción para sacar al jugador de la sala y actualizar el contador de miembros.
        firestore.runTransaction(transaction -> {
            DocumentSnapshot roomSnap = transaction.get(roomRef);
            Long playersCount = roomSnap.getLong("playersCount");
            long safeCount = playersCount != null ? playersCount : 0;

            if (safeCount > 0) {
                transaction.update(roomRef, "playersCount", safeCount - 1);
            }

            transaction.delete(memberRef);
            return null;
        }).addOnSuccessListener(unused -> {
            finish();
        }).addOnFailureListener(e -> {
            showToast("Leave failed: " + e.getMessage());
            finish();
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // Liberar los listeners activos al cerrar la pantalla para evitar fugas y actualizaciones innecesarias.
        if (roomListener != null) {
            roomListener.remove();
        }
        if (membersListener != null) {
            membersListener.remove();
        }
    }

}