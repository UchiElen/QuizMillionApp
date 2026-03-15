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

import com.dam.quizmillionapp.BaseActivity;
import com.dam.quizmillionapp.R;
import com.dam.quizmillionapp.adapters.RoomsAdapter;
import com.dam.quizmillionapp.auth.UserSession;
import com.dam.quizmillionapp.interfaces.CreateRoomCallback;
import com.dam.quizmillionapp.interfaces.JoinRoomCallback;
import com.dam.quizmillionapp.interfaces.LoadRoomsCallback;
import com.dam.quizmillionapp.models.RoomSummary;
import com.dam.quizmillionapp.repositories.RoomRepository;
import com.dam.quizmillionapp.repositories.UserRepository;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.List;

public class LobbyActivity extends BaseActivity {
    private ListenerRegistration roomsListener;
    private RoomsAdapter roomsAdapter;
    private EditText edtRoomCode;
    private Button btnJoinByCode;
    private Button btnCreateRoom;
    private RoomRepository roomRepository;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lobby);

        roomRepository = new RoomRepository();

        edtRoomCode = findViewById(R.id.edtRoomCode);
        btnJoinByCode = findViewById(R.id.btnJoinByCode);
        btnCreateRoom = findViewById(R.id.btnCreateRoom);

        RecyclerView rvRooms = findViewById(R.id.rvRooms);
        rvRooms.setLayoutManager(new LinearLayoutManager(this));

        roomsAdapter = new RoomsAdapter(new RoomsAdapter.OnRoomClickListener() {
            @Override
            public void onRoomClicked(RoomSummary room) {
                joinRoomByRoomId(room.getRoomId());
            }
        });

        rvRooms.setAdapter(roomsAdapter);

        btnCreateRoom.setOnClickListener(view -> createNewRoom("Room"));

        btnJoinByCode.setOnClickListener(view -> {
            String code = edtRoomCode.getText().toString().trim().toUpperCase();

            if (code.length() != 6) {
                showToast("El código de la sala debe tener 6 caracteres.");
                return;
            }

            joinRoomByCode(code);
        });

        startRoomsRealtimeListener();
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private void startRoomsRealtimeListener() {
        roomsListener = roomRepository.listenAvailableRooms(new LoadRoomsCallback() {
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

    private void createNewRoom(String roomName) {

        String uid = UserSession.getCurrentUid(this);

        if (uid == null || uid.trim().isEmpty()) {
            showToast("No se pudo obtener el usuario actual.");
            return;
        }

        UserRepository userRepository = new UserRepository();
        userRepository.getUserNameByUid(uid, new UserRepository.OnUserNameLoadedCallback() {

            @Override
            public void onSuccess(String userName) {

                roomRepository.createRoom(roomName, uid, userName, new CreateRoomCallback() {
                    @Override
                    public void onSuccess(String roomId) {
                        openRoom(roomId);
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

    private void joinRoomByRoomId(String roomId) {

        String currentUid = UserSession.getCurrentUid(this);

        if (currentUid == null || currentUid.trim().isEmpty()) {
            showToast("No se pudo obtener el usuario actual.");
            return;
        }

        UserRepository userRepository = new UserRepository();
        userRepository.getUserNameByUid(currentUid, new UserRepository.OnUserNameLoadedCallback() {
            @Override
            public void onSuccess(String userName) {
                roomRepository.joinRoomByRoomId(roomId, currentUid, userName, new JoinRoomCallback() {
                    @Override
                    public void onSuccess(String roomId, boolean alreadyJoined) {
                        if (alreadyJoined) {
                            showToast("Ya estás en esta sala.");
                        }
                        openRoom(roomId);
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

    private void joinRoomByCode(String roomCode) {
        String currentUid = UserSession.getCurrentUid(this);

        if (currentUid == null || currentUid.trim().isEmpty()) {
            showToast("No se pudo obtener el usuario actual.");
            return;
        }

        UserRepository userRepository = new UserRepository();
        userRepository.getUserNameByUid(currentUid, new UserRepository.OnUserNameLoadedCallback() {
            @Override
            public void onSuccess(String userName) {
                roomRepository.joinRoomByCode(roomCode, currentUid, userName, new JoinRoomCallback() {
                    @Override
                    public void onSuccess(String roomId, boolean alreadyJoined) {
                        if (alreadyJoined) {
                            showToast("Ya estás en esta sala.");
                        }
                        openRoom(roomId);
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

    private void openRoom(String roomId) {
        Intent intent = new Intent(this, WaitingActivity.class);
        intent.putExtra("roomId", roomId);
        startActivity(intent);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (roomsListener != null) {
            roomsListener.remove();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (edtRoomCode != null) {
            edtRoomCode.setText("");
        }
    }









}