package com.dam.quizmillionapp;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.firebase.firestore.FirebaseFirestore;

public class ResultadoActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private TextView tvBanner;
    private ImageView imgCromo;
    private Button btnMenuPrincipal, btnPuntuaciones;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_resultado);

        // 1. Inicializar vistas
        tvBanner = findViewById(R.id.tv_banner_mensaje);
        imgCromo = findViewById(R.id.img_premio);
        btnMenuPrincipal = findViewById(R.id.btn_menu_principal);
        btnPuntuaciones = findViewById(R.id.btn_ver_puntuaciones);

        // 2. Inicializar Firebase
        db = FirebaseFirestore.getInstance();

        // 3. Obtener el premio que viene de la partida
        int premioConseguido = getIntent().getIntExtra("PREMIO", 0);

        // 4. Cargar datos desde Firebase
        cargarDatosPremio(premioConseguido);

        // 5. Configurar botones
        btnMenuPrincipal.setOnClickListener(v -> {
            Intent intent = new Intent(ResultadoActivity.this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            finish();
        });

        btnPuntuaciones.setOnClickListener(v -> {
            // Aquí iría tu Intent a la pantalla de Ranking/Puntuaciones
            Toast.makeText(this, "Cargando Ranking...", Toast.LENGTH_SHORT).show();
        });
    }

    private void cargarDatosPremio(int cifra) {
        // Consultamos el documento que tiene como ID el valor del premio (ej: "1500")
        db.collection("premios").document(String.valueOf(cifra))
                .get()
                .addOnSuccessListener(document -> {
                    if (document.exists()) {
                        // Extraemos los datos de Firebase
                        String titulo = document.getString("titulo");
                        String colorHex = document.getString("colorHex");
                        String urlImagen = document.getString("imagenUrl");

                        // Aplicamos los textos y colores
                        tvBanner.setText(titulo != null ? titulo : "¡BIEN JUGADO!");
                        if (colorHex != null) {
                            tvBanner.setBackgroundColor(Color.parseColor(colorHex));
                        }

                        // Cargamos la imagen con Glide
                        Glide.with(this)
                                .load(urlImagen)
                                .placeholder(android.R.drawable.progress_horizontal) // Icono mientras carga
                                .error(R.drawable.cromo_0) // Imagen por defecto si falla
                                .into(imgCromo);

                    } else {
                        // Caso por defecto si no existe el premio en la BBDD
                        configurarVistaError();
                    }
                })
                .addOnFailureListener(e -> {
                    configurarVistaError();
                    Toast.makeText(this, "Error al conectar con la base de datos", Toast.LENGTH_SHORT).show();
                });
    }

    private void configurarVistaError() {
        tvBanner.setText("¡FIN DEL JUEGO!");
        tvBanner.setBackgroundColor(Color.parseColor("#444444"));
        imgCromo.setImageResource(R.drawable.cromo_0); // Imagen local de reserva
    }
}