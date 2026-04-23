package com.dam.quizmillionapp.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.dam.quizmillionapp.R;
import com.dam.quizmillionapp.models.MemberListItem;

import java.util.ArrayList;
import java.util.List;

public class MembersAdapter extends RecyclerView.Adapter<MembersAdapter.MemberViewHolder> {

    private final List<MemberListItem> members = new ArrayList<>();

    // Actualiza la lista completa de jugadores que hay en la sala
    public void updateMembers(List<MemberListItem> newMembers) {
        members.clear();

        if (newMembers != null) {
            members.addAll(newMembers);
        }

        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public MemberViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View row = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_member_row, parent, false);

        return new MemberViewHolder(row);
    }

    @Override
    public void onBindViewHolder(@NonNull MemberViewHolder holder, int position) {
        MemberListItem member = members.get(position);

        holder.txtMemberName.setText(member.getDisplayName());

        // Si el jugador es el host mostramos el icono
        if (member.isHost()) {
            holder.imgHost.setVisibility(View.VISIBLE);
        } else {
            holder.imgHost.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return members.size();
    }

    static class MemberViewHolder extends RecyclerView.ViewHolder {

        TextView txtMemberName;
        ImageView imgHost;

        MemberViewHolder(@NonNull View itemView) {
            super(itemView);

            txtMemberName = itemView.findViewById(R.id.txtPlayerName);
            imgHost = itemView.findViewById(R.id.imgHost);
        }
    }
}