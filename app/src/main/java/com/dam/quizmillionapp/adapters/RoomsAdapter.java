package com.dam.quizmillionapp.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.dam.quizmillionapp.R;
import com.dam.quizmillionapp.models.RoomSummary;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;

public class RoomsAdapter extends RecyclerView.Adapter<RoomsAdapter.RoomViewHolder> {

    public interface OnRoomClickListener {
        void onRoomClicked(RoomSummary room);
    }

    private final List<RoomSummary> roomList = new ArrayList<>();
    private final OnRoomClickListener clickListener;

    public RoomsAdapter(OnRoomClickListener clickListener) {
        this.clickListener = clickListener;
    }

    public void updateRooms(List<RoomSummary> newRooms) {
        roomList.clear();
        roomList.addAll(newRooms);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public RoomViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View row = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_room_row, parent, false);

        return new RoomViewHolder(row);
    }

    @Override
    public void onBindViewHolder(@NonNull RoomViewHolder holder, int position) {
        RoomSummary room = roomList.get(position);

        holder.txtRoomName.setText("Room " + room.getCode());

        holder.txtRoomCode.setText(
                holder.itemView.getContext().getString(R.string.room_code_prefix) + " " + room.getCode()
        );

        holder.txtPlayers.setText(
                holder.itemView.getContext().getString(R.string.players_prefix) + " "
                        + room.getPlayerCount() + " / " + room.getMaxPlayers()
        );

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                clickListener.onRoomClicked(room);
            }
        });

        holder.btnJoinRoom.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                clickListener.onRoomClicked(room);
            }
        });
    }

    @Override
    public int getItemCount() {
        return roomList.size();
    }

    public static class RoomViewHolder extends RecyclerView.ViewHolder {
        TextView txtRoomName;
        TextView txtRoomCode;
        TextView txtPlayers;
        MaterialButton btnJoinRoom;

        public RoomViewHolder(@NonNull View itemView) {
            super(itemView);
            txtRoomName = itemView.findViewById(R.id.txtRoomName);
            txtRoomCode = itemView.findViewById(R.id.txtRoomCode);
            txtPlayers = itemView.findViewById(R.id.txtPlayers);
            btnJoinRoom = itemView.findViewById(R.id.btnJoinRoom);
        }
    }
}