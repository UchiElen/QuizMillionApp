package com.dam.quizmillionapp.adapters; // Paquete.

import android.view.LayoutInflater; // Para inflar layout.
import android.view.View; // Vista.
import android.view.ViewGroup; // Contenedor.
import android.widget.TextView; // TextView.

import androidx.annotation.NonNull; // NoNull.
import androidx.recyclerview.widget.RecyclerView; // Recycler.

import com.dam.quizmillionapp.R;

import java.util.ArrayList; // ArrayList.
import java.util.List; // List.

public class MembersAdapter extends RecyclerView.Adapter<MembersAdapter.MemberViewHolder> { // Adaptador de jugadores.

    private final List<String> memberNames = new ArrayList<>(); // Lista interna de nombres.

    public void updateMembers(List<String> newNames) { // Actualiza la lista de miembros.
        memberNames.clear(); // Limpia lista anterior.
        memberNames.addAll(newNames); // Añade nuevos nombres.
        notifyDataSetChanged(); // Redibuja lista.
    }

    @NonNull
    @Override
    public MemberViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) { // Crea fila.
        View row = LayoutInflater.from(parent.getContext()) // Obtiene inflater.
                .inflate(R.layout.item_member_row, parent, false); // Infla el XML del miembro.
        return new MemberViewHolder(row); // Devuelve el holder.
    }

    @Override
    public void onBindViewHolder(@NonNull MemberViewHolder holder, int position) { // Rellena fila.
        String name = memberNames.get(position); // Obtiene el nombre en esta posición.
        holder.txtMemberName.setText(name); // Lo pinta en pantalla.
    }

    @Override
    public int getItemCount() { // Devuelve tamaño de lista.
        return memberNames.size(); // Cantidad de miembros.
    }

    static class MemberViewHolder extends RecyclerView.ViewHolder { // Holder del TextView.
        TextView txtMemberName; // Referencia al TextView.

        MemberViewHolder(@NonNull View itemView) { // Constructor.
            super(itemView); // Llama al padre.
            txtMemberName = itemView.findViewById(R.id.txtMemberName); // Enlaza el TextView.
        }
    }
}