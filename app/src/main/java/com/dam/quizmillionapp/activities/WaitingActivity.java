package com.dam.quizmillionapp.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.dam.quizmillionapp.AuthActivity;
import com.dam.quizmillionapp.BaseActivity;
import com.dam.quizmillionapp.PreguntasActivity;
import com.dam.quizmillionapp.R;
import com.dam.quizmillionapp.ResetActivity;
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

import java.util.List;

public class WaitingActivity extends BaseActivity {

    private String roomId;
    private ListenerRegistration roomListener;

    private ListenerRegistration membersListener;
    private TextView txtRoomCode;
    private TextView txtRoomStatus;
    private Button btnStartGame;
    private Button btnLeaveRoom;
    private MembersAdapter membersAdapter;
    private RoomRepository roomRepository;
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

        txtRoomCode = findViewById(R.id.txtRoomCode);
        txtRoomStatus = findViewById(R.id.txtRoomStatus);

        btnStartGame = findViewById(R.id.btnStartGame);
        btnLeaveRoom = findViewById(R.id.btnLeaveRoom);

        RecyclerView rvMembers = findViewById(R.id.rvMembers);
        rvMembers.setLayoutManager(new LinearLayoutManager(this));
        membersAdapter = new MembersAdapter();

        rvMembers.setAdapter(membersAdapter);

        btnStartGame.setOnClickListener(view -> startGameIfHost());
        btnLeaveRoom.setOnClickListener(view -> leaveRoomAndExit());

        startRealtimeListeners();
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private void startRealtimeListeners() {
        roomListener = roomRepository.listenRoomDetails(roomId, new LoadRoomDetailsCallback() {
            @Override
            public void onRoomLoaded(String code, String status, String hostUid) {


                if (code != null) {
                    txtRoomCode.setText("Code: " + code);
                } else {
                    txtRoomCode.setText("Code: ------");
                }



                String statusText;

                if (status == null) {

                    statusText = "Preparando la sala...";

                } else {

                    switch (status) {

                        case RoomStatus.OPEN:
                            statusText = "Esperando jugadores...";
                            break;

                        case RoomStatus.FULL:
                            statusText = "Sala llena";
                            break;

                        case RoomStatus.IN_GAME:
                            statusText = "Partida en curso";
                            break;

                        case RoomStatus.FINISHED:
                            statusText = "Partida finalizada";
                            break;

                        case RoomStatus.CANCELLED:
                            statusText = "Sala cancelada";
                            break;

                        default:
                            statusText = "Estado desconocido";
                            break;
                    }
                }

                txtRoomStatus.setText(statusText);



                String myUid = UserSession.getCurrentUid(WaitingActivity.this);
                boolean isHost = myUid != null && myUid.equals(hostUid);

                boolean canStart = isHost &&
                        ( RoomStatus.OPEN.equals(status) || RoomStatus.FULL.equals(status) );
                btnStartGame.setEnabled(canStart);

                if (RoomStatus.IN_GAME.equals(status) && !hasNavigatedToQuestions) {
                    hasNavigatedToQuestions = true;

                    Intent intent = new Intent(WaitingActivity.this, PreguntasActivity.class);
                    intent.putExtra("roomId", roomId);
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
                membersAdapter.updateMembers(memberNames);
            }

            @Override
            public void onError(String errorMessage) {
                showToast(errorMessage);
            }
        });
    }

    private void startGameIfHost() {
        String myUid = UserSession.getCurrentUid(this);

        if (myUid == null || myUid.trim().isEmpty()) {
            showToast("No se pudo obtener el usuario actual.");
            return;
        }

        roomRepository.startGameIfHost(roomId, myUid, new StartGameCallback() {
            @Override
            public void onSuccess() {
                showToast("Partida iniciada!");
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

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (roomListener != null) {
            roomListener.remove();
        }

        if (membersListener != null) {
            membersListener.remove();
        }
    }


}