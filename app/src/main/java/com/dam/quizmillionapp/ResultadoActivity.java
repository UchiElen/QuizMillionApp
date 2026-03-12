package com.dam.quizmillionapp;

import android.graphics.drawable.Drawable;
import androidx.annotation.Nullable;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import androidx.constraintlayout.widget.ConstraintLayout;
import android.view.View;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.Target;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

public class ResultadoActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private TextView tvBanner, tvMensaje;
    private ImageView imgPremio;

    private ConstraintLayout layoutCarga;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_resultado);

        // Enlaces con el XML
        tvBanner = findViewById(R.id.tv_banner_mensaje);
        tvMensaje = findViewById(R.id.tv_frase_graciosa);
        imgPremio = findViewById(R.id.img_premio);
        layoutCarga = findViewById(R.id.layout_carga);

        db = FirebaseFirestore.getInstance();

        // Recuperamos el nivel enviado (0 a 15)
        int nivel = getIntent().getIntExtra("NIVEL_ALCANZADO", 0);

        // Llamada a la base de datos
        consultarPremio(nivel);

        // Botón para salir
        findViewById(R.id.btn_menu_principal).setOnClickListener(v -> finish());
    }

    private void consultarPremio(int nivelAlcanzado) {
        db.collection("premios")
                .whereEqualTo("nivel", nivelAlcanzado)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {

                            // 1. Extraer datos...
                            String titulo = doc.getString("titulo");
                            String colorHex = doc.getString("banner");
                            String mensaje = doc.getString("mensaje");
                            String urlFirebase = doc.getString("img");

                            // 2. Aplicar textos y color (Esto no tarda)
                            tvBanner.setText(titulo);
                            tvMensaje.setText(mensaje);
                            try {
                                tvBanner.setBackgroundColor(Color.parseColor(colorHex));
                            } catch (Exception e) {
                                tvBanner.setBackgroundColor(Color.RED);
                            }

                            // 3. CARGA CON GLIDE CON TRANSICIÓN (Igual que en preguntas)
                            if (urlFirebase != null && !urlFirebase.isEmpty()) {
                                Glide.with(this)
                                        .load(urlFirebase) // Recuerda: usa la URL directa, sin el método convertir
                                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                                        .listener(new RequestListener<Drawable>() {
                                            @Override
                                            public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                                                // Log para saber qué ha fallado exactamente
                                                Log.e("GLIDE_ERROR", "Error al cargar: " + (e != null ? e.getMessage() : "Desconocido"));

                                                ocultarCargaConAnimacion();
                                                return false; // Importante: dejar en false para que Glide gestione el error internamente
                                            }

                                            @Override
                                            public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                                                // ¡Todo bien!
                                                ocultarCargaConAnimacion();
                                                return false; // Importante: dejar en false para que la imagen se pinte en el ImageView
                                            }
                                        })
                                        .into(imgPremio);
                            } else {
                                // Si no hay imagen, quitamos la carga directamente
                                ocultarCargaConAnimacion();
                            }
                        }
                    } else {
                        // Si el nivel no existe, quitamos la carga y mostramos el error
                        ocultarCargaConAnimacion();
                        tvBanner.setText("NIVEL " + nivelAlcanzado + " NO ENCONTRADO");
                    }
                })
                .addOnFailureListener(e -> {
                    ocultarCargaConAnimacion();
                    Toast.makeText(this, "Error de red", Toast.LENGTH_SHORT).show();
                });
    }

    // NUEVO MÉTODO PARA HACER EL EFECTO DE DESVANECIDO
    private void ocultarCargaConAnimacion() {
        layoutCarga.animate()
                .alpha(0.0f) // Se vuelve transparente
                .setDuration(500) // Tarda medio segundo
                .withEndAction(() -> layoutCarga.setVisibility(View.GONE)) // Y luego desaparece
                .start();
    }

}