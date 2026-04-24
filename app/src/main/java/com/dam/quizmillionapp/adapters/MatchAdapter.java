package com.dam.quizmillionapp.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.dam.quizmillionapp.Match;
import com.dam.quizmillionapp.R;

import java.util.List;

public class MatchAdapter extends RecyclerView.Adapter<MatchAdapter.MatchViewHolder> {
    private List<Match> matches;

    public MatchAdapter(List<Match> matches) {
        this.matches = matches;
    }

    @NonNull
    @Override
    public MatchViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.partidas_card_view, parent, false);
        return new MatchViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull MatchViewHolder holder, int position) {
        Match m = matches.get(position);
        holder.tvFecha.setText(m.getFechaFormateada());
        holder.tvPuntos.setText(m.getScore() + " €");
        holder.tvDetalles.setText("Nivel Alcanzado: " + m.getLevelReached() + " | Modo de Juego: " + m.getMode());

        // destacar con una estrella la puntuación mas alta:
        if (position == 0) {
            holder.imgStar.setVisibility(View.VISIBLE);
        } else {
            holder.imgStar.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() { return matches.size(); }

    static class MatchViewHolder extends RecyclerView.ViewHolder {
        TextView tvFecha, tvPuntos, tvDetalles;
        android.widget.ImageView imgStar;

        public MatchViewHolder(@NonNull View itemView) {
            super(itemView);
            tvFecha = itemView.findViewById(R.id.tvFecha);
            tvPuntos = itemView.findViewById(R.id.tvPuntos);
            tvDetalles = itemView.findViewById(R.id.tvDetalles);
            imgStar = itemView.findViewById(R.id.imgStar);
        }
    }
}