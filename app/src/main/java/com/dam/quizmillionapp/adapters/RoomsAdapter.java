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
    public RoomsAdapter(OnRoomClickListener clickListener){
        this.clickListener = clickListener;
    }
    public void updateRooms(List<RoomSummary> newRooms){
        roomList.clear();
        roomList.addAll(newRooms);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public RoomsAdapter.RoomViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View row = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_room_row, parent, false);

        return new RoomViewHolder(row);
    }

    @Override
    public void onBindViewHolder(@NonNull RoomsAdapter.RoomViewHolder holder, int position) {
        RoomSummary room = roomList.get(position);
        // 1. Mostramos la información principal de la sala
        holder.txtRoomName.setText(room.getRoomName());
        holder.txtRoomCode.setText(
                holder.itemView.getContext().getString(R.string.room_code_prefix) + " "
                        + room.getRoomCode()
        );

        holder.txtPlayers.setText(
                holder.itemView.getContext().getString(R.string.players_prefix) + " "
                        + room.getPlayersCount() + " / " + room.getPlayersMax()
        );
        // 2. Se permite la entrada toando la fila completa
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                clickListener.onRoomClicked(room);
            }
        });

        // 3. Se permite la entrada pulsando el notón 'Unirse'
        holder.btnJoinRoom.setOnClickListener(new View.OnClickListener(){
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

    // Esta clase representa una fila del RecyclerView.
    // Guarda las referencias a los elementos de la interfaz donde se muestran los datos de la sala.
    public static class RoomViewHolder extends RecyclerView.ViewHolder{
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