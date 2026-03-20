package com.dam.quizmillionapp;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.dam.quizmillionapp.activities.LobbyActivity;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;

public class PuntuacionesActivity extends BaseActivity {
    private String roomId;
    private FirebaseFirestore db;
    private RecyclerView rvPuntuaciones;
    private PuntuacionesAdapter adapter;
    private TextView tvContadorFinalizados;
    private List<PlayerScore> listaJugadores;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_puntuaciones);
        tvContadorFinalizados = findViewById(R.id.tv_contador_finalizados);

        roomId = getIntent().getStringExtra("roomId");
        if (roomId == null || roomId.trim().isEmpty()) {
            Toast.makeText(this, "Error: Sala no encontrada", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        db = FirebaseFirestore.getInstance();
        rvPuntuaciones = findViewById(R.id.rv_puntuaciones);
        rvPuntuaciones.setLayoutManager(new LinearLayoutManager(this));

        listaJugadores = new ArrayList<>();
        adapter = new PuntuacionesAdapter(listaJugadores);
        rvPuntuaciones.setAdapter(adapter);

        configurarBotones();
        cargarPuntuaciones();
    }

    private void configurarBotones() {
        Button btnMenu = findViewById(R.id.btn_menu_principal);
        Button btnVolver = findViewById(R.id.btn_volver_jugar);

        btnMenu.setOnClickListener(v -> {
            SoundManager.getInstance(this).playClick();
            Intent intent = new Intent(this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });

        btnVolver.setOnClickListener(v -> {
            SoundManager.getInstance(this).playClick();
            Intent intent = new Intent(this, LobbyActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
    }

    private void cargarPuntuaciones() {
        db.collection("rooms").document(roomId).collection("members")
                .addSnapshotListener((queryDocumentSnapshots, e) -> {
                    if (e != null || queryDocumentSnapshots == null) return;

                    int totalJugadores = queryDocumentSnapshots.size();
                    final int[] hanTerminadoContador = {0};

                    List<PlayerScore> nuevaLista = new ArrayList<>();
                    AtomicInteger procesados = new AtomicInteger(0);

                    for (QueryDocumentSnapshot memberDoc : queryDocumentSnapshots) {
                        Long score = memberDoc.getLong("score");
                        Boolean terminado = memberDoc.getBoolean("terminado");

                        if (Boolean.TRUE.equals(terminado)) {
                            hanTerminadoContador[0]++;
                        }

                        String uid = memberDoc.getId();
                        final Long finalScore = (score == null) ? 0L : score;

                        db.collection("usuarios").document(uid).get().addOnCompleteListener(taskFirestore -> {
                            String nombreTemp = "Jugador";
                            if (taskFirestore.isSuccessful() && taskFirestore.getResult() != null && taskFirestore.getResult().exists()) {
                                nombreTemp = taskFirestore.getResult().getString("nombreUsuario");
                            } else {
                                nombreTemp = memberDoc.getString("displayName");
                            }

                            final String nombreFinal = nombreTemp;

                            StorageReference fotoRef = FirebaseStorage.getInstance().getReference().child("users/" + uid + "/profile.jpg");

                            fotoRef.getDownloadUrl().addOnCompleteListener(taskStorage -> {
                                String fotoUrl = null;
                                if (taskStorage.isSuccessful() && taskStorage.getResult() != null) {
                                    fotoUrl = taskStorage.getResult().toString();
                                }
                                synchronized (listaJugadores) {
                                    nuevaLista.add(new PlayerScore(nombreFinal, finalScore, fotoUrl));

                                    if (procesados.incrementAndGet() == totalJugadores) {
                                        nuevaLista.sort((p1, p2) -> p2.puntuacion.compareTo(p1.puntuacion));

                                        listaJugadores.clear();
                                        listaJugadores.addAll(nuevaLista);
                                        adapter.notifyDataSetChanged();

                                        int finalizados = hanTerminadoContador[0];
                                        tvContadorFinalizados.setText("Finalizados: " + finalizados + " / " + totalJugadores);

                                        if (finalizados == totalJugadores) {
                                            tvContadorFinalizados.setText("¡Todos han terminado!");
                                            tvContadorFinalizados.setTextColor(Color.GREEN);
                                        } else {
                                            tvContadorFinalizados.setTextColor(Color.WHITE);
                                        }
                                    }
                                }
                            });
                        });
                    }
                });
    }

    private static class PlayerScore {
        String nombre;
        Long puntuacion;
        String fotoUrl;

        PlayerScore(String nombre, Long puntuacion, String fotoUrl) {
            this.nombre = nombre;
            this.puntuacion = puntuacion;
            this.fotoUrl = fotoUrl;
        }
    }

    private static class PuntuacionesAdapter extends RecyclerView.Adapter<PuntuacionesAdapter.ViewHolder> {
        private final List<PlayerScore> jugadores;

        PuntuacionesAdapter(List<PlayerScore> jugadores) {
            this.jugadores = jugadores;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_puntuacion, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            PlayerScore jugador = jugadores.get(position);
            holder.tvNombre.setText(jugador.nombre);

            NumberFormat format = NumberFormat.getInstance(new Locale("es", "ES"));
            String scoreFormateado = format.format(jugador.puntuacion).replace(".", " ") + " €";
            holder.tvPremio.setText(scoreFormateado);
            holder.ivMedalla.setVisibility(View.VISIBLE);
            Glide.with(holder.itemView.getContext()).clear(holder.ivMedalla);

            if (position == 0) {
                holder.ivMedalla.setImageResource(R.drawable.medal_gold);
            } else if (position == 1) {
                holder.ivMedalla.setImageResource(R.drawable.medal_silver);
            } else if (position == 2) {
                holder.ivMedalla.setImageResource(R.drawable.medal_bronze);
            } else {
                if (jugador.fotoUrl != null) {
                    Glide.with(holder.itemView.getContext())
                            .load(jugador.fotoUrl)
                            .circleCrop()
                            .error(R.drawable.mascot)
                            .into(holder.ivMedalla);
                } else {
                    holder.ivMedalla.setImageResource(R.drawable.mascot);
                }
            }
        }

        @Override
        public int getItemCount() { return jugadores.size(); }
        static class ViewHolder extends RecyclerView.ViewHolder {
            ImageView ivMedalla;
            TextView tvPremio, tvNombre;

            ViewHolder(View itemView) {
                super(itemView);
                ivMedalla = itemView.findViewById(R.id.iv_medalla);
                tvPremio = itemView.findViewById(R.id.tv_premio);
                tvNombre = itemView.findViewById(R.id.tv_nombre_usuario);
            }
        }
    }
}