package com.dam.quizmillionapp.activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
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

    private final Handler presenceHandler = new Handler();
    private Runnable presenceRunnable;

    private ArrayList<String> selectedCategories;

    // Evita abrir varias veces la pantalla de preguntas
    private boolean hasNavigatedToQuestions = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_waiting);

        roomRepository = new RoomRepository();

        roomId = getIntent().getStringExtra("roomId");

        if (roomId == null || roomId.trim().isEmpty()) {
            showToast("RoomId perdido");
            finish();
            return;
        }

        selectedCategories = getIntent().getStringArrayListExtra("selectedCategories");
        if (selectedCategories == null) {
            selectedCategories = new ArrayList<>();
        }

        bindViews();
        setupMembersList();
        setupActions();

        // Al entrar en la sala corregimos posibles desajustes
        // por si quedó alguien inactivo o el contador no era real.
        roomRepository.removeInactivePlayers(roomId);
        roomRepository.fixRoomStateIfNeeded(roomId);

        startPresenceUpdate();
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
        btnStartGame.setOnClickListener(view -> tryStartGame());
        btnLeaveRoom.setOnClickListener(view -> leaveRoomAndExit());
    }

    private void startRealtimeListeners() {

        roomListener = roomRepository.observeRoomInfo(roomId, new LoadRoomDetailsCallback() {
            @Override
            public void onRoomLoaded(String code,
                                     String roomName,
                                     boolean isPublic,
                                     String status,
                                     String hostUid,
                                     int loadedMaxPlayers) {

                maxPlayers = loadedMaxPlayers;

                updateRoomHeader(roomName, code, isPublic);
                updatePlayersTitle();
                txtRoomStatus.setText(getStatusText(status));

                boolean isHost = isCurrentUserHost(hostUid);
                boolean canStart = canCurrentUserStart(status, isHost);

                btnStartGame.setEnabled(canStart);

                // Cuando la sala pasa a IN_GAME, todos los jugadores
                // deben avanzar automáticamente a la pantalla de preguntas.
                if (RoomStatus.IN_GAME.equals(status) && !hasNavigatedToQuestions) {
                    openQuestionsScreen();
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

        membersListener = roomRepository.observeRoomMembers(roomId, new LoadMembersCallback() {
            @Override
            public void onMembersLoaded(List<MemberListItem> memberNames) {

                membersAdapter.updateMembers(memberNames);

                if (memberNames != null) {
                    currentPlayers = memberNames.size();
                } else {
                    currentPlayers = 0;
                }

                updatePlayersTitle();
            }

            @Override
            public void onError(String errorMessage) {
                showToast(errorMessage);
            }
        });
    }

    private boolean isCurrentUserHost(String hostUid) {
        String myUid = UserSession.getCurrentUid(this);
        return myUid != null && myUid.equals(hostUid);
    }

    private boolean canCurrentUserStart(String status, boolean isHost) {
        if (!isHost) {
            return false;
        }

        boolean validStatus = RoomStatus.OPEN.equals(status) || RoomStatus.FULL.equals(status);
        return validStatus && currentPlayers >= 2;
    }

    private void openQuestionsScreen() {
        hasNavigatedToQuestions = true;

        Intent intent = new Intent(WaitingActivity.this, PreguntasActivity.class);
        intent.putExtra("roomId", roomId);
        intent.putStringArrayListExtra("selectedCategories", selectedCategories);
        startActivity(intent);
        finish();
    }

    private String getStatusText(String status) {
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

        String finalRoomName;
        if (roomName != null && !roomName.trim().isEmpty()) {
            finalRoomName = roomName.trim();
        } else {
            finalRoomName = "Sala sin nombre";
        }

        txtRoomName.setText(finalRoomName);

        if (isPublic) {
            txtRoomCode.setVisibility(View.GONE);
            txtRoomCode.setText("");
        } else {
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

    private void tryStartGame() {
        String myUid = UserSession.getCurrentUid(this);

        if (myUid == null || myUid.trim().isEmpty()) {
            showToast("No se pudo obtener el usuario actual.");
            return;
        }

        roomRepository.tryStartGame(roomId, myUid, new StartGameCallback() {
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

    // Cada cierto tiempo actualizamos la actividad del usuario.
    // Así podemos detectar desconexiones y limpiar jugadores inactivos.
    private void startPresenceUpdate() {

        presenceRunnable = new Runnable() {
            @Override
            public void run() {
                String uid = UserSession.getCurrentUid(WaitingActivity.this);

                if (uid != null && roomId != null) {
                    roomRepository.updateUserActivity(roomId, uid);
                    roomRepository.removeInactivePlayers(roomId);
                }

                presenceHandler.postDelayed(this, 30000);
            }
        };

        presenceHandler.post(presenceRunnable);
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (roomListener != null) {
            roomListener.remove();
        }

        if (membersListener != null) {
            membersListener.remove();
        }

        // Al cerrar la pantalla quitamos listeners y tareas pendientes
        // para no dejar procesos vivos en segundo plano.
        if (presenceRunnable != null) {
            presenceHandler.removeCallbacks(presenceRunnable);
        }
    }
}