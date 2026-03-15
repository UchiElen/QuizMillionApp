package com.dam.quizmillionapp;

import android.content.Intent;
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
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

/**
 * Clase que gestiona la pantalla final de premios.
 * Se encarga de mostrar el premio obtenido consultando a Firestore según el nivel.
 */
public class ResultadoActivity extends BaseActivity {

    private FirebaseFirestore db;
    private TextView tvBanner, tvMensaje;
    private ImageView imgPremio;
    private ConstraintLayout layoutCarga;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_resultado);

        // Inicializamos los objetos de la interfaz
        tvBanner = findViewById(R.id.tv_banner_mensaje);
        tvMensaje = findViewById(R.id.tv_frase_graciosa);
        imgPremio = findViewById(R.id.img_premio);
        layoutCarga = findViewById(R.id.layout_carga);

        db = FirebaseFirestore.getInstance();

        // Recuperamos el nivel donde el usuario se plantó o perdió
        // Si hay algún error, por defecto mostramos el nivel 0
        int nivel = getIntent().getIntExtra("NIVEL_ALCANZADO", 0);

        // Lanzamos la consulta a firebase storage
        consultarPremio(nivel);

        // Listener para volver al inicio y cerrar esta activity
        findViewById(R.id.btn_menu_principal).setOnClickListener(v -> {
            Intent intent = new Intent(ResultadoActivity.this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
    }

    /**
     * Busca en la colección "premios" el documento que coincida con el nivel del alcanzado
     */
    private void consultarPremio(int nivelAlcanzado) {
        db.collection("premios")
                .whereEqualTo("nivel", nivelAlcanzado)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        // Aunque sea un for, solo debería venir un premio por nivel
                        for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {

                            // Extrae los campos tal cual están en la consola de Firebase
                            String titulo = doc.getString("titulo");
                            String colorHex = doc.getString("banner");
                            String mensaje = doc.getString("mensaje");
                            String urlFirebase = doc.getString("img"); // URL directa de Firebase Storage

                            // Fijamos textos básicos
                            tvBanner.setText(titulo);
                            tvMensaje.setText(mensaje);

                            // Intentamos parsear el color del campo banner (fondo del mensaje de la cabecera)
                            try {
                                tvBanner.setBackgroundColor(Color.parseColor(colorHex));
                            } catch (Exception e) {
                                // Fallback por si el HEX de la DB está mal escrito o falta
                                tvBanner.setBackgroundColor(Color.DKGRAY);
                            }

                            // Gestión de las imagenes con la herramienta Glide
                            if (urlFirebase != null && !urlFirebase.isEmpty()) {
                                Glide.with(this)
                                        .load(urlFirebase)
                                        .diskCacheStrategy(DiskCacheStrategy.ALL) // Guardamos en caché para no gastar datos si repite
                                        .listener(new RequestListener<Drawable>() {
                                            @Override
                                            public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                                                Log.e("PROYECTO_LOG", "Fallo al cargar imagen de premio: " + urlFirebase);
                                                ocultarCargaConAnimacion(); // Quitamos el loader aunque falle
                                                return false;
                                            }

                                            @Override
                                            public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                                                // Cuando la imagen está lista, ocultamos el layout de carga
                                                ocultarCargaConAnimacion();
                                                return false;
                                            }
                                        })
                                        .into(imgPremio);
                            } else {
                                // Si no hay URL definida, quitamos el loader para que no se quede infinitamente
                                ocultarCargaConAnimacion();
                            }
                        }
                    } else {
                        // Caso de error: nivel no registrado en la base de datos, aunque no es probable pero cubre fallo humano al alimentar la bbdd
                        ocultarCargaConAnimacion();
                        tvBanner.setText("¡Nivel " + nivelAlcanzado + "!");
                        tvMensaje.setText("Aún no hay premio registrado para este nivel.");
                    }
                })
                .addOnFailureListener(e -> {
                    // Error crítico de conexión con Firestore
                    ocultarCargaConAnimacion();
                    Toast.makeText(this, "Error al conectar con el servidor", Toast.LENGTH_SHORT).show();
                });
    }

    /**
     * Animación suave para ocultar la pantalla de carga porque las imagenes a veces tardan
     */

    private void ocultarCargaConAnimacion() {
        if (layoutCarga != null && layoutCarga.getVisibility() == View.VISIBLE) {
            layoutCarga.animate()
                    .alpha(0.0f) // Desvanecimiento
                    .setDuration(400) // 400ms es un tiempo natural para el ojo
                    .withEndAction(() -> layoutCarga.setVisibility(View.GONE))
                    .start();
        }
    }
}