package com.dam.quizmillionapp.adapters;

import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.dam.quizmillionapp.R;
import com.dam.quizmillionapp.constants.RoomStatus;
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

        // =========================
        // NOMBRE
        // =========================
        String roomName = room.getName();
        if (roomName == null || roomName.trim().isEmpty()) {
            roomName = "Sala sin nombre";
        }

        holder.txtRoomName.setText(roomName);

        // =========================
        // ESTADO
        // =========================
        String status = room.getStatus();
        String statusText;
        int statusColor;

        if (RoomStatus.OPEN.equals(status)) {
            statusText = "Esperando";
            statusColor = holder.itemView.getContext().getColor(R.color.green);
        } else if (RoomStatus.IN_GAME.equals(status)) {
            statusText = "En partida";
            statusColor = holder.itemView.getContext().getColor(R.color.orange);
        } else {
            statusText = "Finalizada";
            statusColor = holder.itemView.getContext().getColor(R.color.red);
        }

        holder.txtRoomStatus.setText(statusText);
        holder.txtRoomStatus.setTextColor(statusColor);

        // =========================
        // JUGADORES
        // =========================
        holder.txtPlayers.setText(
                "Jugadores: " + room.getPlayerCount() + " / " + room.getMaxPlayers()
        );

        // =========================
        // LÓGICA DE UNIÓN
        // =========================
        boolean isOpen = RoomStatus.OPEN.equals(status);
        boolean hasSpace = room.getPlayerCount() < room.getMaxPlayers();
        boolean canJoin = isOpen && hasSpace;

        if (canJoin) {

            // Botón activo
            holder.btnJoinRoom.setEnabled(true);
            holder.btnJoinRoom.setAlpha(1.0f);
            holder.btnJoinRoom.setText("Unirse");

            holder.btnJoinRoom.setBackgroundTintList(
                    ColorStateList.valueOf(
                            holder.itemView.getContext().getColor(R.color.green)
                    )
            );

            holder.btnJoinRoom.setTextColor(
                    holder.itemView.getContext().getColor(R.color.white)
            );

            // Click activo
            holder.itemView.setOnClickListener(view ->
                    clickListener.onRoomClicked(room)
            );

            holder.btnJoinRoom.setOnClickListener(view ->
                    clickListener.onRoomClicked(room)
            );

        } else {

            // Botón desactivado
            holder.btnJoinRoom.setEnabled(false);
            holder.btnJoinRoom.setAlpha(0.6f);

            holder.btnJoinRoom.setBackgroundTintList(
                    ColorStateList.valueOf(
                            holder.itemView.getContext().getColor(R.color.gray)
                    )
            );

            holder.btnJoinRoom.setTextColor(
                    holder.itemView.getContext().getColor(R.color.white)
            );

            if (!isOpen) {
                holder.btnJoinRoom.setText("No disponible");
            } else {
                holder.btnJoinRoom.setText("Llena");
            }

            // Quitar clicks
            holder.itemView.setOnClickListener(null);
            holder.btnJoinRoom.setOnClickListener(null);
        }
    }

    @Override
    public int getItemCount() {
        return roomList.size();
    }

    public static class RoomViewHolder extends RecyclerView.ViewHolder {

        TextView txtRoomName;
        TextView txtRoomStatus;
        TextView txtPlayers;
        MaterialButton btnJoinRoom;

        public RoomViewHolder(@NonNull View itemView) {
            super(itemView);

            txtRoomName = itemView.findViewById(R.id.txtRoomName);
            txtRoomStatus = itemView.findViewById(R.id.txtRoomStatus);
            txtPlayers = itemView.findViewById(R.id.txtPlayers);
            btnJoinRoom = itemView.findViewById(R.id.btnJoinRoom);
        }
    }
}