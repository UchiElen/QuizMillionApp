package com.dam.quizmillionapp.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.dam.quizmillionapp.BaseActivity;
import com.dam.quizmillionapp.R;
import com.dam.quizmillionapp.adapters.RoomsAdapter;
import com.dam.quizmillionapp.auth.UserSession;
import com.dam.quizmillionapp.interfaces.JoinRoomCallback;
import com.dam.quizmillionapp.interfaces.LoadRoomsCallback;
import com.dam.quizmillionapp.models.RoomSummary;
import com.dam.quizmillionapp.repositories.RoomRepository;
import com.dam.quizmillionapp.repositories.UserRepository;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.List;
import java.util.Locale;

public class LobbyActivity extends BaseActivity {

    private EditText edtRoomCode;
    private Button btnJoinByCode;
    private Button btnCreateRoom;
    private RecyclerView rvRooms;

    private RoomRepository roomRepository;
    private RoomsAdapter roomsAdapter;
    private ListenerRegistration roomsListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lobby);

        roomRepository = new RoomRepository();

        bindViews();
        setupRoomsList();
        setupActions();
        observeOpenRooms();
    }

    private void bindViews() {
        edtRoomCode = findViewById(R.id.edtRoomCode);
        btnJoinByCode = findViewById(R.id.btnJoinByCode);
        btnCreateRoom = findViewById(R.id.btnCreateRoom);
        rvRooms = findViewById(R.id.rvRooms);
    }

    private void setupRoomsList() {
        rvRooms.setLayoutManager(new LinearLayoutManager(this));

        roomsAdapter = new RoomsAdapter(new RoomsAdapter.OnRoomClickListener() {
            @Override
            public void onRoomClicked(RoomSummary room) {
                tryJoinRoomById(room.getRoomId());
            }
        });

        rvRooms.setAdapter(roomsAdapter);
    }

    private void setupActions() {

        btnCreateRoom.setOnClickListener(view -> openRoomConfig());

        btnJoinByCode.setOnClickListener(view -> {
            String roomCode = edtRoomCode.getText().toString().trim().toUpperCase(Locale.ROOT);

            if (roomCode.length() != 6) {
                showToast("El código de la sala debe tener 6 caracteres.");
                return;
            }

            tryJoinRoomByCode(roomCode);
        });
    }

    // El lobby escucha las salas públicas en tiempo real
    // para que la lista se refresque sola.
    private void observeOpenRooms() {
        roomsListener = roomRepository.observeOpenRooms(new LoadRoomsCallback() {
            @Override
            public void onRoomsLoaded(List<RoomSummary> roomList) {
                roomsAdapter.updateRooms(roomList);
            }

            @Override
            public void onError(String errorMessage) {
                showToast("Error: " + errorMessage);
            }
        });
    }

    private void openRoomConfig() {
        Intent intent = new Intent(LobbyActivity.this, RoomConfigActivity.class);
        startActivity(intent);
    }

    private void tryJoinRoomById(String roomId) {
        String uid = UserSession.getCurrentUid(this);

        if (uid == null || uid.trim().isEmpty()) {
            showToast("No se pudo obtener el usuario actual.");
            return;
        }

        loadUserNameAndJoinById(roomId, uid);
    }

    private void loadUserNameAndJoinById(String roomId, String uid) {
        UserRepository userRepository = new UserRepository();

        // Antes de entrar guardamos también el nombre del jugador
        // para que la sala lo muestre correctamente en la lista de miembros.
        userRepository.getUserNameByUid(uid, new UserRepository.OnUserNameLoadedCallback() {
            @Override
            public void onSuccess(String userName) {
                roomRepository.joinRoomDirectly(roomId, uid, userName, new JoinRoomCallback() {
                    @Override
                    public void onSuccess(String joinedRoomId, boolean alreadyJoined) {
                        if (alreadyJoined) {
                            showToast("Ya estás en esta sala.");
                        }

                        openWaitingRoom(joinedRoomId);
                    }

                    @Override
                    public void onError(String errorMessage) {
                        showToast(errorMessage);
                    }
                });
            }

            @Override
            public void onError(String errorMessage) {
                showToast(errorMessage);
            }
        });
    }

    private void tryJoinRoomByCode(String roomCode) {
        String uid = UserSession.getCurrentUid(this);

        if (uid == null || uid.trim().isEmpty()) {
            showToast("No se pudo obtener el usuario actual.");
            return;
        }

        loadUserNameAndJoinByCode(roomCode, uid);
    }

    private void loadUserNameAndJoinByCode(String roomCode, String uid) {
        UserRepository userRepository = new UserRepository();

        userRepository.getUserNameByUid(uid, new UserRepository.OnUserNameLoadedCallback() {
            @Override
            public void onSuccess(String userName) {
                roomRepository.joinRoomUsingCode(roomCode, uid, userName, new JoinRoomCallback() {
                    @Override
                    public void onSuccess(String joinedRoomId, boolean alreadyJoined) {
                        if (alreadyJoined) {
                            showToast("Ya estás en esta sala.");
                        }

                        openWaitingRoom(joinedRoomId);
                    }

                    @Override
                    public void onError(String errorMessage) {
                        showToast(errorMessage);
                    }
                });
            }

            @Override
            public void onError(String errorMessage) {
                showToast(errorMessage);
            }
        });
    }

    private void openWaitingRoom(String roomId) {
        Intent intent = new Intent(this, WaitingActivity.class);
        intent.putExtra("roomId", roomId);
        startActivity(intent);
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Al volver al lobby limpiamos el campo
        // para que no se quede el código anterior escrito.
        if (edtRoomCode != null) {
            edtRoomCode.setText("");
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (roomsListener != null) {
            roomsListener.remove();
        }
    }
}