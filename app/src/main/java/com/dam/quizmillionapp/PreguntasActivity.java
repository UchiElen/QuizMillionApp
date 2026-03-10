package com.dam.quizmillionapp;

import android.content.Intent;
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
import com.dam.quizmillionapp.BaseActivity;
import com.dam.quizmillionapp.MusicService;
import com.dam.quizmillionapp.MainActivity;
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

    private ImageButton btn50, btnPublico, btnLlamada;
    private ImageButton btnMusica, btnAbandonar;
    private MaterialButton btnPlantarse;

    private int contadorFallos = 0;
    private int nivelActual = 1;
    private int indicePregunta = 0;

    private boolean usado50 = false;
    private boolean usadoPublico = false;
    private boolean usadoLlamada = false;

    private List<Pregunta> listaPreguntasNivel = new ArrayList<>();
    private Pregunta preguntaActual;
    private CountDownTimer reloj;
    private FirebaseFirestore db;

    private androidx.constraintlayout.widget.ConstraintLayout layoutTransicion;
    private TextView tvTransicionTitulo, tvTransicionMensaje, tvTransicionPremio;

    private ProgressBar pbProgreso;
    private TextView tvPremioActual;
    private final int[] escalaPremios = {0, 100, 250, 500, 750, 1500, 2500, 5000, 10000, 15000, 20000, 30000, 50000, 100000, 300000, 1000000};
    private final int COLOR_AMBAR = Color.parseColor("#FFC107");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_preguntas);

        // Enlace de vistas (IMPORTANTE: Primero enlazamos, luego usamos)
        progressLoader = findViewById(R.id.progress_loader);
        tvCronometro = findViewById(R.id.tv_cronometro);
        tvEnunciado = findViewById(R.id.tv_enunciado);
        tvFallos = findViewById(R.id.tv_fallos);
        imgPregunta = findViewById(R.id.img_pregunta);
        btnAbandonar = findViewById(R.id.btn_abandonar);
        btnPlantarse = findViewById(R.id.btn_plantarse);

        // Configurar icono inicial según el estado global de la música
        // 1. PRIMERO: Enlazar la vista
        btnMusica = findViewById(R.id.btn_musica);
        // 2. SEGUNDO: Aplicar el icono heredado del estado global

        layoutTransicion = findViewById(R.id.layout_transicion_nivel);
        tvTransicionTitulo = findViewById(R.id.tv_texto_transicion_titulo);
        tvTransicionMensaje = findViewById(R.id.tv_texto_transicion_mensaje);
        tvTransicionPremio = findViewById(R.id.tv_texto_transicion_premio);

        btnOpciones[0] = findViewById(R.id.btn_opcion_1);
        btnOpciones[1] = findViewById(R.id.btn_opcion_2);
        btnOpciones[2] = findViewById(R.id.btn_opcion_3);
        btnOpciones[3] = findViewById(R.id.btn_opcion_4);

        btn50 = findViewById(R.id.btn_50);
        btnPublico = findViewById(R.id.btn_publico);
        btnLlamada = findViewById(R.id.btn_llamada);

        pbProgreso = findViewById(R.id.pb_progreso_juego);
        tvPremioActual = findViewById(R.id.tv_premio_actual);
        pbProgreso.setMax(15);

        // Listeners
        btnPlantarse.setOnClickListener(v -> mensajePlantarse());
        btnAbandonar.setOnClickListener(v -> mensajeAbandonar());
        btnMusica.setOnClickListener(v -> toggleMusica());
        btn50.setOnClickListener(v -> comodin50());
        btnPublico.setOnClickListener(v -> comodinPublico());
        btnLlamada.setOnClickListener(v -> comodinLlamada());

        for (int i = 0; i < btnOpciones.length; i++) {
            final int finalI = i;
            btnOpciones[i].setOnClickListener(v -> validarRespuesta(finalI));
        }

        db = FirebaseFirestore.getInstance();
        mostrarTransicionYNivel();
    }

    private void cargarNivelCompleto() {
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
                        prepararDatosPregunta();
                    } else {
                        Toast.makeText(this, "Error cargando preguntas", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void prepararDatosPregunta() {
        if (indicePregunta >= listaPreguntasNivel.size()) {
            Collections.shuffle(listaPreguntasNivel);
            indicePregunta = 0;
        }
        preguntaActual = listaPreguntasNivel.get(indicePregunta);

        // IMPORTANTE: Asegurar que el enunciado y las opciones vuelvan a ser visibles
        tvEnunciado.setVisibility(View.VISIBLE);
        btnPlantarse.setVisibility(View.VISIBLE);

        for (MaterialButton btn : btnOpciones) {
            btn.setVisibility(View.VISIBLE); // <--- ESTO ES CLAVE
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
            progressLoader.setVisibility(View.VISIBLE);
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
                            iniciarReloj();
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
        btnPlantarse.setVisibility(View.INVISIBLE);

        if (seleccionado == preguntaActual.correcta) {
            if (reloj != null) reloj.cancel();
            btnOpciones[seleccionado].setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#4CAF50")));
            btnOpciones[seleccionado].setTextColor(Color.WHITE);

            if (nivelActual == 15) {
                new Handler().postDelayed(() -> irAResultados(15), 1500);
                return;
            }

            pbProgreso.setProgress(nivelActual);
            tvPremioActual.setText("NIVEL " + nivelActual + " > Premio: " + escalaPremios[nivelActual] + " €");

            if (nivelActual == 5) pbProgreso.setProgressTintList(ColorStateList.valueOf(Color.GREEN));
            if (nivelActual == 10) pbProgreso.setProgressTintList(ColorStateList.valueOf(Color.RED));

            nivelActual++;
            new Handler().postDelayed(this::mostrarTransicionYNivel, 1500);
        } else {
            btnOpciones[seleccionado].setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#F44336")));
            btnOpciones[seleccionado].setTextColor(Color.WHITE);
            btnOpciones[seleccionado].setEnabled(false);
            actualizarFallos();
        }
    }

    private void mostrarTransicionYNivel() {
        tvTransicionTitulo.setText("NIVEL " + nivelActual);

        // Personalizamos el mensaje si es un nivel de "seguro"
        if (nivelActual == 6 || nivelActual == 11) {
            tvTransicionMensaje.setText("¡HAS LOGRADO UN SEGURO!\n Ahora juegas por :");
            tvTransicionMensaje.setTextColor(Color.parseColor("#FFC107"));
        } else {
            tvTransicionMensaje.setText("Juegas por");
            tvTransicionMensaje.setTextColor(Color.WHITE); // Color normal
        }

        tvTransicionPremio.setText(escalaPremios[nivelActual] + "€");

        layoutTransicion.setVisibility(View.VISIBLE);
        layoutTransicion.setAlpha(1.0f);

        cargarNivelCompleto();

        new Handler().postDelayed(() -> {
            layoutTransicion.animate().alpha(0.0f).setDuration(500).withEndAction(() -> {
                layoutTransicion.setVisibility(View.GONE);
                mostrarSiguientePregunta();
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
                btnPlantarse.setVisibility(View.INVISIBLE);
                tvCronometro.setText("0");

                // Mostramos la correcta en verde para que el usuario aprenda
                btnOpciones[preguntaActual.correcta].setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#4CAF50")));

                actualizarFallos();

                // Solo si no ha perdido la partida, pasamos a la siguiente
                if (contadorFallos < 3) {
                    new Handler().postDelayed(() -> {
                        ocultarElementosParaCarga(); // Los ocultamos un momento para el efecto de carga
                        indicePregunta++; // Pasamos a la siguiente pregunta del mismo nivel
                        prepararDatosPregunta(); // <--- LLAMAR ESTO AQUÍ garantiza que vuelvan a aparecer
                        mostrarSiguientePregunta(); // Carga la imagen e inicia el reloj
                    }, 1500);
                }
            }
        }.start();
    }

    private void actualizarFallos() {
        contadorFallos++;
        tvFallos.setText("Fallos: " + contadorFallos + "/3");
        if (contadorFallos >= 3) {
            if (reloj != null) reloj.cancel();
            int premioConsolacion = (nivelActual >= 10) ? 10 : (nivelActual >= 5 ? 5 : 0);
            irAResultados(premioConsolacion);
        }
    }

    private void mensajePlantarse() {
        btnPlantarse.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#FFC107")));
        btnPlantarse.setTextColor(Color.BLACK);

        int nivelParaEnviar = nivelActual - 1;
        int dinero = (nivelActual > 1) ? escalaPremios[nivelActual - 1] : 0;

        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("¿Te plantas?")
                .setMessage("Tu premio acumulado es de " + dinero + " €")
                .setCancelable(false)
                .setPositiveButton("Si, estoy seguro", (dialog, which) -> {
                    if (reloj != null) reloj.cancel();
                    irAResultados(nivelParaEnviar);
                })
                .setNegativeButton("Me lo he pensado mejor, continuo", (dialog, which) -> {
                    btnPlantarse.setBackgroundTintList(ColorStateList.valueOf(Color.TRANSPARENT));
                    btnPlantarse.setTextColor(Color.WHITE);
                    dialog.dismiss();
                })
                .show();
    }

    private void irAResultados(int nivel) {
        Intent intent = new Intent(this, ResultadoActivity.class);
        intent.putExtra("NIVEL_ALCANZADO", nivel);
        startActivity(intent);
        finish();
    }

    private void mensajeAbandonar() {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Salir")
                .setMessage("¿Seguro que quieres salir? Perderás el progreso.")
                .setPositiveButton("Salir", (d, w) -> finish())
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void ocultarElementosParaCarga() {
        tvEnunciado.setVisibility(View.INVISIBLE);
        imgPregunta.setVisibility(View.INVISIBLE);
        for (MaterialButton btn : btnOpciones) btn.setVisibility(View.INVISIBLE);
    }

    public void toggleMusica() {

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

    private void comodin50() {
        if (usado50 || preguntaActual == null) return;
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
        int sug = preguntaActual.comodin_publico;
        btnOpciones[sug].setBackgroundTintList(ColorStateList.valueOf(COLOR_AMBAR));
        Toast.makeText(this, "El público dice la " + (sug + 1), Toast.LENGTH_SHORT).show();
    }

    private void comodinLlamada() {
        if (usadoLlamada || preguntaActual == null) return;
        usadoLlamada = true;
        desactivaBotonComodin(btnLlamada);
        int sug = preguntaActual.comodin_llamada;
        btnOpciones[sug].setBackgroundTintList(ColorStateList.valueOf(COLOR_AMBAR));
        Toast.makeText(this, "Tu amigo cree que es la " + (sug + 1), Toast.LENGTH_SHORT).show();
    }
}