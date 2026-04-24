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
    private int maxScore = -1; // Variable para guardar la puntuación más alta

    public MatchAdapter(List<Match> matches) {
        this.matches = matches;
        calcularMaximo();
    }

    public void setMatches(List<Match> newMatches) {
        this.matches = newMatches;
        calcularMaximo();
        notifyDataSetChanged(); // refrescar la lista en pantalla
    }

    // guardarse el dato de puntuacion maxima para destacar dicho resultado
    private void calcularMaximo() {
        this.maxScore = -1;
        if (matches != null && !matches.isEmpty()) {
            for (Match m : matches) {
                this.maxScore = Math.max(this.maxScore, m.getScore());
            }
        }
    }

    @NonNull
    @Override
    public MatchViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.partidas_card_view, parent, false);
        return new MatchViewHolder(v);
    }

    // organizacion de las tarjetas y sus datos
    @Override
    public void onBindViewHolder(@NonNull MatchViewHolder holder, int position) {
        Match m = matches.get(position);
        holder.tvFecha.setText(m.getFechaFormateada());
        holder.tvPuntos.setText(m.getScore() + " €");
        holder.tvDetalles.setText("Nivel Alcanzado: " + m.getLevelReached() + " | Modo de Juego: " + m.getMode());

        // destacar con una estrella la puntuación más alta
        if (m.getScore() == maxScore && maxScore > 0) {
            holder.imgStar.setVisibility(View.VISIBLE);
        } else {
            holder.imgStar.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() { return matches != null ? matches.size() : 0; }

    // declarar y enlazar vistas
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