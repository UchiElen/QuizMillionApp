package com.dam.quizmillionapp;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PreguntasActivity extends AppCompatActivity {

    private ProgressBar progressLoader;
    private TextView tvCronometro, tvEnunciado, tvFallos;
    private ImageView imgPregunta;
    private MaterialButton[] btnOpciones = new MaterialButton[4];

    // Botones de comodines
    private ImageButton btn50, btnPublico, btnLlamada;

    // Nuevos controles superiores
    private ImageButton btnMusica, btnAbandonar;

    private int contadorFallos = 0;
    private int nivelActual = 1;
    private int indicePregunta = 0;

    private boolean usado50 = false;
    private boolean usadoPublico = false;
    private boolean usadoLlamada = false;
    private boolean musicaEncendida = true;

    private List<Pregunta> listaPreguntasNivel = new ArrayList<>();
    private Pregunta preguntaActual;
    private CountDownTimer reloj;
    private FirebaseFirestore db;

    // Capa de transición
    private androidx.constraintlayout.widget.ConstraintLayout layoutTransicion;
    private TextView tvTransicionTitulo, tvTransicionPremio;

    private ProgressBar pbProgreso;
    private TextView tvPremioActual;
    private final int[] escalaPremios = {0, 100, 250, 500, 750, 1500, 2500, 5000, 10000, 15000, 20000, 30000, 50000, 100000, 300000, 1000000};
    private final int COLOR_AMBAR = Color.parseColor("#FFC107");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_preguntas);

        // Enlace de vistas principales
        progressLoader = findViewById(R.id.progress_loader);
        tvCronometro = findViewById(R.id.tv_cronometro);
        tvEnunciado = findViewById(R.id.tv_enunciado);
        tvFallos = findViewById(R.id.tv_fallos);
        imgPregunta = findViewById(R.id.img_pregunta);

        // Enlace de controles superiores
        btnAbandonar = findViewById(R.id.btn_abandonar);
        btnMusica = findViewById(R.id.btn_musica);

        // Enlace de capa de transición
        layoutTransicion = findViewById(R.id.layout_transicion_nivel);
        tvTransicionTitulo = findViewById(R.id.tv_texto_transicion_titulo);
        tvTransicionPremio = findViewById(R.id.tv_texto_transicion_premio);

        // Enlace de botones de respuesta
        btnOpciones[0] = findViewById(R.id.btn_opcion_1);
        btnOpciones[1] = findViewById(R.id.btn_opcion_2);
        btnOpciones[2] = findViewById(R.id.btn_opcion_3);
        btnOpciones[3] = findViewById(R.id.btn_opcion_4);

        // Enlace de botones de comodines
        btn50 = findViewById(R.id.btn_50);
        btnPublico = findViewById(R.id.btn_publico);
        btnLlamada = findViewById(R.id.btn_llamada);

        pbProgreso = findViewById(R.id.pb_progreso_juego);
        tvPremioActual = findViewById(R.id.tv_premio_actual);
        pbProgreso.setMax(15);

        // Listeners
        btnAbandonar.setOnClickListener(v -> mostrarDialogoAbandono());
        btnMusica.setOnClickListener(v -> toggleMusica());
        btn50.setOnClickListener(v -> comodin50());
        btnPublico.setOnClickListener(v -> comodinPublico());
        btnLlamada.setOnClickListener(v -> comodinLlamada());

        for (int i = 0; i < btnOpciones.length; i++) {
            final int finalI = i;
            btnOpciones[i].setOnClickListener(v -> validarRespuesta(finalI));
        }

        db = FirebaseFirestore.getInstance();

        // Iniciamos con la transición del Nivel 1
        mostrarTransicionYNivel();
    }

    private void cargarNivelCompleto() {
        // No mostramos el loader aquí porque ya tenemos la capa de transición tapando
        db.collection("preguntas")
                .whereEqualTo("nivel", nivelActual)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null && !task.getResult().isEmpty()) {
                        listaPreguntasNivel.clear();
                        for (QueryDocumentSnapshot doc : task.getResult()) {
                            listaPreguntasNivel.add(doc.toObject(Pregunta.class));
                        }
                        Collections.shuffle(listaPreguntasNivel);
                        indicePregunta = 0;
                        // Preparamos los datos, pero no iniciamos reloj hasta que la transición acabe
                        prepararDatosPregunta();
                    } else {
                        Toast.makeText(this, "Error: No hay preguntas en nivel " + nivelActual, Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void prepararDatosPregunta() {
        if (indicePregunta >= listaPreguntasNivel.size()) {
            Collections.shuffle(listaPreguntasNivel);
            indicePregunta = 0;
        }
        preguntaActual = listaPreguntasNivel.get(indicePregunta);

        // Reset de la UI de los botones (Se mantiene texto blanco por defecto)
        for (MaterialButton btn : btnOpciones) {
            btn.setVisibility(View.VISIBLE);
            btn.setEnabled(true);
            btn.setAlpha(1.0f);
            btn.setBackgroundTintList(ColorStateList.valueOf(Color.TRANSPARENT));
            btn.setStrokeColor(ColorStateList.valueOf(Color.WHITE));
            btn.setTextColor(Color.WHITE);
        }

        tvEnunciado.setText(preguntaActual.enunciado);
        for (int i = 0; i < 4; i++) {
            btnOpciones[i].setText(preguntaActual.opciones.get(i));
        }
    }

    private void mostrarSiguientePregunta() {
        tvEnunciado.setVisibility(View.VISIBLE);

        if (preguntaActual.imagen != null && !preguntaActual.imagen.isEmpty()) {
            imgPregunta.setVisibility(View.VISIBLE);
            progressLoader.setVisibility(View.VISIBLE); // Loader pequeño interno mientras Glide descarga

            Glide.with(this)
                    .load(convertirUrlDrive(preguntaActual.imagen))
                    .listener(new RequestListener<Drawable>() {
                        @Override
                        public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                            progressLoader.setVisibility(View.GONE);
                            iniciarReloj();
                            return false;
                        }

                        @Override
                        public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                            progressLoader.setVisibility(View.GONE);
                            iniciarReloj(); // EL TIEMPO EMPIEZA CUANDO LA IMAGEN ESTÁ LISTA
                            return false;
                        }
                    })
                    .into(imgPregunta);
        } else {
            imgPregunta.setVisibility(View.GONE);
            iniciarReloj();
        }
    }

    private void validarRespuesta(int seleccionado) {
        if (seleccionado == preguntaActual.correcta) {
            if (reloj != null) reloj.cancel();
            btnOpciones[seleccionado].setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#4CAF50")));
            btnOpciones[seleccionado].setTextColor(Color.WHITE);

            if (nivelActual <= 15) {
                pbProgreso.setProgress(nivelActual);
                tvPremioActual.setText("NIVEL " + nivelActual + " > Premio acumulado: " + escalaPremios[nivelActual] + " €");
                if (nivelActual == 5) pbProgreso.setProgressTintList(ColorStateList.valueOf(Color.GREEN));
                if (nivelActual == 10) pbProgreso.setProgressTintList(ColorStateList.valueOf(Color.RED));
            }

            nivelActual++;
            // Lanzamos transición del siguiente nivel tras el color verde de acierto
            new Handler().postDelayed(this::mostrarTransicionYNivel, 1500);
        } else {
            btnOpciones[seleccionado].setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#F44336")));
            btnOpciones[seleccionado].setTextColor(Color.WHITE);
            btnOpciones[seleccionado].setEnabled(false);
            actualizarFallos();
        }
    }

    private void mostrarTransicionYNivel() {
        if (nivelActual > 15) {
            Toast.makeText(this, "¡ERES MILLONARIO!", Toast.LENGTH_LONG).show();
            return;
        }

        tvTransicionTitulo.setText("NIVEL " + nivelActual);
        tvTransicionPremio.setText(escalaPremios[nivelActual] + "€");

        layoutTransicion.setVisibility(View.VISIBLE);
        layoutTransicion.setAlpha(1.0f);

        // Cargamos nivel de Firebase mientras el usuario ve el premio
        cargarNivelCompleto();

        new Handler().postDelayed(() -> {
            layoutTransicion.animate().alpha(0.0f).setDuration(500).withEndAction(() -> {
                layoutTransicion.setVisibility(View.GONE);
                mostrarSiguientePregunta(); // Ahora sí, mostramos la pregunta cargada
            });
        }, 2000);
    }

    private void iniciarReloj() {
        if (reloj != null) reloj.cancel();
        reloj = new CountDownTimer(30000, 1000) {
            public void onTick(long millisUntilFinished) {
                tvCronometro.setText("" + millisUntilFinished / 1000);
                if (millisUntilFinished < 6000) tvCronometro.setTextColor(Color.RED);
                else tvCronometro.setTextColor(Color.WHITE);
            }
            public void onFinish() {
                tvCronometro.setText("0");
                btnOpciones[preguntaActual.correcta].setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#4CAF50")));
                btnOpciones[preguntaActual.correcta].setTextColor(Color.WHITE);
                actualizarFallos();
                if (contadorFallos < 3) {
                    indicePregunta++;
                    new Handler().postDelayed(() -> {
                        ocultarElementosParaCarga();
                        mostrarSiguientePregunta();
                    }, 1500);
                }
            }
        }.start();
    }

    private void mostrarDialogoAbandono() {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("¿Abandonar partida?")
                .setMessage("Si sales ahora perderás tu progreso actual.")
                .setPositiveButton("Sí, salir", (dialog, which) -> {
                    if (reloj != null) reloj.cancel();
                    finish();
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void toggleMusica() {
        musicaEncendida = !musicaEncendida;
        btnMusica.setImageResource(musicaEncendida ? R.drawable.ic_music_on : R.drawable.ic_music_off);
        // Aquí es donde conectarás con tu MediaPlayer cuando lo tengas listo
    }

    private void actualizarFallos() {
        contadorFallos++;
        tvFallos.setText("Fallos: " + contadorFallos + "/3");
        if (contadorFallos >= 3) {
            if (reloj != null) reloj.cancel();
            Toast.makeText(this, "¡GAME OVER!", Toast.LENGTH_LONG).show();
            new Handler().postDelayed(this::finish, 1000);
        }
    }

    private void ocultarElementosParaCarga() {
        tvEnunciado.setVisibility(View.INVISIBLE);
        imgPregunta.setVisibility(View.INVISIBLE);
        for (MaterialButton btn : btnOpciones) btn.setVisibility(View.INVISIBLE);
    }

    private String convertirUrlDrive(String url) {
        if (url != null && url.contains("id=")) return url;
        try {
            String id = url.split("/d/")[1].split("/")[0];
            return "https://docs.google.com/uc?export=download&id=" + id;
        } catch (Exception e) { return url; }
    }

    private void desactivaBotonComodin(View v) {
        v.setEnabled(false);
        v.setAlpha(0.3f);
    }

    // --- MÉTODOS DE COMODINES (RESIDUOS DE LOGICA ANTERIOR MANTENIDOS) ---
    private void comodin50() {
        if (usado50 || preguntaActual == null || preguntaActual.comodin_50 == null) return;
        usado50 = true;
        desactivaBotonComodin(btn50);
        for (int i = 0; i < 4; i++) {
            if (preguntaActual.comodin_50.contains(i)) {
                btnOpciones[i].setBackgroundTintList(ColorStateList.valueOf(COLOR_AMBAR));
                btnOpciones[i].setTextColor(Color.BLACK);
            } else {
                btnOpciones[i].setEnabled(false);
                btnOpciones[i].setAlpha(0.3f);
            }
        }
    }

    private void comodinPublico() {
        if (usadoPublico || preguntaActual == null) return;
        usadoPublico = true;
        desactivaBotonComodin(btnPublico);
        int sugerencia = preguntaActual.comodin_publico;
        btnOpciones[sugerencia].setBackgroundTintList(ColorStateList.valueOf(COLOR_AMBAR));
        btnOpciones[sugerencia].setTextColor(Color.BLACK);
        Toast.makeText(this, "El público vota la opción " + (sugerencia + 1), Toast.LENGTH_SHORT).show();
    }

    private void comodinLlamada() {
        if (usadoLlamada || preguntaActual == null) return;
        usadoLlamada = true;
        desactivaBotonComodin(btnLlamada);
        int sugerencia = preguntaActual.comodin_llamada;
        btnOpciones[sugerencia].setBackgroundTintList(ColorStateList.valueOf(COLOR_AMBAR));
        btnOpciones[sugerencia].setTextColor(Color.BLACK);
        Toast.makeText(this, "Tu amigo sugiere la opción " + (sugerencia + 1), Toast.LENGTH_SHORT).show();
    }
}