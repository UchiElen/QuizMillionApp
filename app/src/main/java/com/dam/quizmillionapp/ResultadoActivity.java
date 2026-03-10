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
                            String urlDrive = doc.getString("img");

                            // 2. Aplicar textos y color (Esto no tarda)
                            tvBanner.setText(titulo);
                            tvMensaje.setText(mensaje);
                            try {
                                tvBanner.setBackgroundColor(Color.parseColor(colorHex));
                            } catch (Exception e) {
                                tvBanner.setBackgroundColor(Color.RED);
                            }

                            // 3. CARGA CON GLIDE CON TRANSICIÓN (Igual que en preguntas)
                            if (urlDrive != null && !urlDrive.isEmpty()) {
                                Glide.with(this)
                                        .load(convertirUrlDrive(urlDrive))
                                        .listener(new RequestListener<Drawable>() {
                                            @Override
                                            public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                                                // Si falla, quitamos la cortina para no bloquear
                                                ocultarCargaConAnimacion();
                                                Toast.makeText(ResultadoActivity.this, "Error cargando cromo", Toast.LENGTH_SHORT).show();
                                                return false; // Dejar que Glide muestre el error() si tienes
                                            }

                                            @Override
                                            public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                                                // ¡IMAGEN LISTA! Quitamos la cortina
                                                ocultarCargaConAnimacion();
                                                return false; // Dejar que Glide muestre la imagen
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

    private String convertirUrlDrive(String url) {
        if (url == null || url.isEmpty()) return "";
        try {
            String fileId = "";
            if (url.contains("/d/")) {
                fileId = url.split("/d/")[1].split("/")[0];
            } else if (url.contains("id=")) {
                fileId = url.split("id=")[1].split("&")[0];
            }
            // Este es el formato de descarga directa que NUNCA falla si el archivo es público
            return "https://drive.google.com/uc?export=download&id=" + fileId;
        } catch (Exception e) {
            return url;
        }
    }
}