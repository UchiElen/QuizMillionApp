package com.dam.quizmillionapp.activities;

import android.content.Intent;
import android.os.Bundle;

import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.dam.quizmillionapp.BaseActivity;
import com.dam.quizmillionapp.PreguntasActivity;
import com.dam.quizmillionapp.R;
import com.dam.quizmillionapp.adapters.MembersAdapter;
import com.dam.quizmillionapp.auth.UserSession;
import com.dam.quizmillionapp.constants.RoomStatus;
import com.dam.quizmillionapp.interfaces.LeaveRoomCallback;
import com.dam.quizmillionapp.interfaces.LoadMembersCallback;
import com.dam.quizmillionapp.interfaces.LoadRoomDetailsCallback;
import com.dam.quizmillionapp.interfaces.StartGameCallback;
import com.dam.quizmillionapp.models.MemberListItem;
import com.dam.quizmillionapp.repositories.RoomRepository;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.List;

public class WaitingActivity extends BaseActivity {

    private String roomId;
    private ListenerRegistration roomListener;
    private ListenerRegistration membersListener;
    private TextView txtRoomName;
    private TextView txtRoomCode;
    private TextView txtRoomStatus;
    private TextView txtPlayersTitle;
    private Button btnStartGame;
    private Button btnLeaveRoom;
    private int currentPlayers = 0;
    private int maxPlayers = 0;
    private MembersAdapter membersAdapter;
    private RoomRepository roomRepository;
    private android.os.Handler presenceHandler = new android.os.Handler();
    private Runnable presenceRunnable;

    private ArrayList<String> selectedCategories;

    // Esto evita navegar varias veces a la pantalla de preguntas
    private boolean hasNavigatedToQuestions = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_waiting);

        // Inicializamos el repositorio
        roomRepository = new RoomRepository();

        // recuperamos el roomId
        roomId = getIntent().getStringExtra("roomId");

        // Sin roomId salimos
        if (roomId == null || roomId.trim().isEmpty()) {
            showToast("RoomId perdido");
            finish();
            return;
        }

        // actualizamos periodicamente la presencia del usuario
        startPresenceUpdate();

        // limpiamos miembros inactivos y corregimos el estado real de la sala
        roomRepository.cleanupInactiveMembers(roomId);
        roomRepository.recalculateRoomState(roomId);

        // guardamos las categorias seleccionadas
        selectedCategories = getIntent().getStringArrayListExtra("selectedCategories");

        if (selectedCategories == null) {
            selectedCategories = new ArrayList<>();
        }

        // Enlazamos a los controles
        bindViews();

        // Listamos los miembros de la sala
        setupMembersList();

        // Comfiguramos los botones
        setupActions();

        // Escuchamos cambios en tiempo real
        startRealtimeListeners();

    }

    private void bindViews() {
        txtRoomName = findViewById(R.id.txtRoomName);
        txtRoomCode = findViewById(R.id.txtRoomCode);
        txtRoomStatus = findViewById(R.id.txtRoomStatus);
        txtPlayersTitle = findViewById(R.id.txtPlayersTitle);

        btnStartGame = findViewById(R.id.btnStartGame);
        btnLeaveRoom = findViewById(R.id.btnLeaveRoom);
    }

    private void setupMembersList() {
        RecyclerView rvMembers = findViewById(R.id.rvMembers);
        rvMembers.setLayoutManager(new LinearLayoutManager(this));

        membersAdapter = new MembersAdapter();
        rvMembers.setAdapter(membersAdapter);
    }

    private void setupActions() {

        // Si el usuario es host y la sala lo permite, podrá iniciar la partida
        btnStartGame.setOnClickListener(view -> startGameIfHost());

        // Este botón saca al usuario de la sala
        btnLeaveRoom.setOnClickListener(view -> leaveRoomAndExit());
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }


    private void startRealtimeListeners() {

        roomListener = roomRepository.listenRoomDetails(roomId, new LoadRoomDetailsCallback() {

            @Override
            public void onRoomLoaded(String code,
                                     String roomName,
                                     boolean isPublic,
                                     String status,
                                     String hostUid,
                                     int loadedMaxPlayers) {

                // Guardamos el aforo máximo
                maxPlayers = loadedMaxPlayers;

                // Actualizamos nombre, código y título de jugadores
                updateRoomHeader(roomName, code, isPublic);
                updatePlayersTitle();

                // Normalizamos el texto del estado
                String statusText = buildStatusText(status);
                txtRoomStatus.setText(statusText);

                // Comprobamos si el usuario actual es el anfitrión
                String myUid = UserSession.getCurrentUid(WaitingActivity.this);
                boolean isHost = false;

                if (myUid != null && myUid.equals(hostUid)) {
                    isHost = true;
                }

                // El botón de iniciar solo debe activarse si:
                // 1) el usuario es el host
                // 2) la sala está abierta o llena
                boolean canStart = false;

                if (isHost) {
                    if (( RoomStatus.OPEN.equals(status) || RoomStatus.FULL.equals(status) ) &&
                    currentPlayers >=2 ) {
                        canStart = true;
                    }
                }

                btnStartGame.setEnabled(canStart);

                // Si la sala inicia la partida vamos a la pantalla de preguntas
                if (RoomStatus.IN_GAME.equals(status) && !hasNavigatedToQuestions) {
                    hasNavigatedToQuestions = true;

                    Intent intent = new Intent(WaitingActivity.this, PreguntasActivity.class);
                    intent.putExtra("roomId", roomId);
                    intent.putStringArrayListExtra("selectedCategories", selectedCategories);
                    startActivity(intent);
                    finish();
                }
            }

            @Override
            public void onRoomNotFound() {
                showToast("Sala no encontrada");
                finish();
            }

            @Override
            public void onError(String errorMessage) {
                showToast(errorMessage);
            }
        });

        membersListener = roomRepository.listenRoomMembers(roomId, new LoadMembersCallback() {
            @Override
            public void onMembersLoaded(List<MemberListItem> memberNames) {

                // Actualizamos la lista de miembros
                membersAdapter.updateMembers(memberNames);

                // Actualizamos el número de jugadores
                if (memberNames != null) {
                    currentPlayers = memberNames.size();
                } else {
                    currentPlayers = 0;
                }

                // Refrescamos contador de los jugadores
                updatePlayersTitle();
            }

            @Override
            public void onError(String errorMessage) {
                showToast(errorMessage);
            }
        });
    }


    private String buildStatusText(String status) {

        if (status == null) {
            return "Preparando la sala...";
        }

        switch (status) {
            case RoomStatus.OPEN:
                return "Esperando jugadores...";

            case RoomStatus.FULL:
                return "Sala llena";

            case RoomStatus.IN_GAME:
                return "Partida en curso";

            case RoomStatus.FINISHED:
                return "Partida finalizada";

            case RoomStatus.CANCELLED:
                return "Sala cancelada";

            default:
                return "Estado desconocido";
        }
    }

    private void updateRoomHeader(String roomName, String code, boolean isPublic) {

        String safeRoomName;

        if (roomName != null && !roomName.trim().isEmpty()) {
            safeRoomName = roomName.trim();
        } else {
            safeRoomName = "Sala sin nombre";
        }

        txtRoomName.setText(safeRoomName);

        // Si la sala es pública entonces ocultamos el código
        if (isPublic) {
            txtRoomCode.setVisibility(View.GONE);
            txtRoomCode.setText("");
        } else {
            // Si la sala es privada entonces también mostramos su código
            txtRoomCode.setVisibility(View.VISIBLE);

            if (code != null && !code.trim().isEmpty()) {
                txtRoomCode.setText("Código: " + code);
            } else {
                txtRoomCode.setText("Código: ------");
            }
        }
    }

    private void updatePlayersTitle() {
        txtPlayersTitle.setText("Jugadores (" + currentPlayers + "/" + maxPlayers + ")");
    }

    private void startGameIfHost() {

        // Obtenemos el uid del usuario actual
        String myUid = UserSession.getCurrentUid(this);

        if (myUid == null || myUid.trim().isEmpty()) {
            showToast("No se pudo obtener el usuario actual.");
            return;
        }

        // Iniciamos la partida
        roomRepository.startGameIfHost(roomId, myUid, new StartGameCallback() {

            @Override
            public void onSuccess() {
                showToast("¡Partida iniciada!");
            }

            @Override
            public void onError(String errorMessage) {
                showToast(errorMessage);
            }
        });
    }

    private void leaveRoomAndExit() {

        String uid = UserSession.getCurrentUid(this);

        // Si no hay mas usuarios cerramos la sala
        if (uid == null || uid.trim().isEmpty()) {
            finish();
            return;
        }

        roomRepository.leaveRoom(roomId, uid, new LeaveRoomCallback() {

            @Override
            public void onSuccess() {
                finish();
            }

            @Override
            public void onError(String errorMessage) {
                showToast(errorMessage);
                finish();
            }
        });
    }

    // Enviamos una señal periódica para indicar que el usuario sigue activo en la sala
    private void startPresenceUpdate() {

        presenceRunnable = new Runnable() {
            @Override
            public void run() {
                String uid = UserSession.getCurrentUid(WaitingActivity.this);

                // Si tenemos usuario y sala válida, actualizamos su última actividad
                if (uid != null && roomId != null) {
                    roomRepository.updatePresence(roomId, uid);

                    // Aprovechamos para limpiar miembros inactivos
                    roomRepository.cleanupInactiveMembers(roomId);
                }

                // Repetimos cada 30 segundos
                presenceHandler.postDelayed(this, 30000);
            }
        };
        // Lanzamos la primera ejecución
        presenceHandler.post(presenceRunnable);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // No dejamos escuchas abiertas
        if (roomListener != null) {
            roomListener.remove();
        }

        if (membersListener != null) {
            membersListener.remove();
        }

        // Eliminamos actualizaciones de presencia para no dejar procesos activos
        if (presenceRunnable != null) {
            presenceHandler.removeCallbacks(presenceRunnable);
        }
    }
}