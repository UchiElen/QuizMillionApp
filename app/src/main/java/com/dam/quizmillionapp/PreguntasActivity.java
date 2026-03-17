package com.dam.quizmillionapp;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.util.Log;
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
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Actividad Principal del Juego.
 * Controla el ciclo de vida de las preguntas, cronómetro, comodines y transiciones de nivel.
 */
public class PreguntasActivity extends BaseActivity {

    // --- Componentes ---
    private ProgressBar progressLoader, pbProgreso;
    private TextView tvCronometro, tvEnunciado, tvFallos, tvPremioActual;
    private ImageView imgPregunta;
    private MaterialButton[] btnOpciones = new MaterialButton[4];
    private ImageButton btn50, btnPublico, btnLlamada, btnMusica, btnAbandonar;
    private MaterialButton btnPlantarse;

    // --- Layouts de Transición (Cortinas) ---
    private androidx.constraintlayout.widget.ConstraintLayout layoutTransicion;
    private TextView tvTransicionTitulo, tvTransicionMensajeLinea1,tvTransicionMensajeLinea2, tvTransicionPremio;

    // --- Lógica de Juego y Datos ---
    private FirebaseFirestore db;
    private List<Pregunta> listaPreguntasNivel = new ArrayList<>();
    private Pregunta preguntaActual;
    private CountDownTimer reloj;

    private int contadorFallos = 0;
    private int nivelActual = 1;
    private int indicePregunta = 0;
    private boolean usado50 = false, usadoPublico = false, usadoLlamada = false;

    private boolean efectoActivo = false; // Bloqueo de seguridad para que no se pisen los efectos

    // --- Constantes de Diseño ---
    private final int[] escalaPremios = {0, 100, 250, 500, 750, 1500, 2500, 5000, 10000, 15000, 20000, 30000, 50000, 100000, 300000, 1000000};
    private final int COLOR_AMBAR = Color.parseColor("#FFC107");
    private static final int COLOR_CYAN = Color.parseColor("#00FFFF");


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_preguntas);

        initViews();       // Enlazamos XML con Java
        initFirestore();    // Inicializamos DB

        // El juego comienza con la cortina del Nivel 1
        mostrarTransicionYNivel();
    }

    private void initViews() {
        progressLoader = findViewById(R.id.progress_loader);
        tvCronometro = findViewById(R.id.tv_cronometro);
        tvEnunciado = findViewById(R.id.tv_enunciado);
        tvFallos = findViewById(R.id.tv_fallos);
        imgPregunta = findViewById(R.id.img_pregunta);
        btnAbandonar = findViewById(R.id.btn_abandonar);
        btnPlantarse = findViewById(R.id.btn_plantarse);
        //btnMusica = findViewById(R.id.btn_musica);

        layoutTransicion = findViewById(R.id.layout_transicion_nivel);
        tvTransicionTitulo = findViewById(R.id.tv_texto_transicion_titulo);
        tvTransicionMensajeLinea1 = findViewById(R.id.tv_texto_transicion_mensaje_1);
        tvTransicionMensajeLinea2 = findViewById(R.id.tv_texto_transicion_mensaje_2);
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
        btn50.setOnClickListener(v -> comodin50());
        btnPublico.setOnClickListener(v -> comodinPublico());
        btnLlamada.setOnClickListener(v -> comodinLlamada());

        for (int i = 0; i < btnOpciones.length; i++) {
            final int finalI = i;
            btnOpciones[i].setOnClickListener(v -> validarRespuesta(finalI));
        }
    }

    private void initFirestore() {
        db = FirebaseFirestore.getInstance();
    }

    // --- GESTIÓN DE DATOS (FIRESTORE) ---

    private void cargarNivelCompleto() {
        // Traemos todas las preguntas del nivel actual de una vez para ahorrar lecturas a Firebase
        db.collection("preguntas")
                .whereEqualTo("nivel", nivelActual)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null && !task.getResult().isEmpty()) {
                        listaPreguntasNivel.clear();
                        for (QueryDocumentSnapshot doc : task.getResult()) {
                            listaPreguntasNivel.add(doc.toObject(Pregunta.class));
                        }
                        // Mezclamos para que no salgan siempre en el mismo orden
                        Collections.shuffle(listaPreguntasNivel);
                        indicePregunta = 0;
                        prepararDatosPregunta();
                    } else {
                        Toast.makeText(this, "Error al cargar nivel", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void prepararDatosPregunta() {
        if (indicePregunta >= listaPreguntasNivel.size()) {
            Collections.shuffle(listaPreguntasNivel);
            indicePregunta = 0;
        }
        preguntaActual = listaPreguntasNivel.get(indicePregunta);

        // Limpieza visual de los elementos para la nueva pregunta
        tvEnunciado.setVisibility(View.VISIBLE);
        btnPlantarse.setVisibility(View.VISIBLE);
        tvEnunciado.setText(preguntaActual.enunciado);

        for (int i = 0; i < 4; i++) {
            btnOpciones[i].setVisibility(View.VISIBLE);
            btnOpciones[i].setEnabled(true);
            btnOpciones[i].setAlpha(1.0f);
            btnOpciones[i].setText(preguntaActual.opciones.get(i));
            btnOpciones[i].setBackgroundTintList(ColorStateList.valueOf(Color.TRANSPARENT));
            btnOpciones[i].setStrokeColor(ColorStateList.valueOf(Color.WHITE));
            btnOpciones[i].setTextColor(Color.WHITE);
        }
    }

    private void mostrarSiguientePregunta() {
        // Gestión de la imagen con Glide (usamos URL directa + token de Firebase Storage)
        if (preguntaActual.imagen != null && !preguntaActual.imagen.isEmpty()) {
            imgPregunta.setVisibility(View.VISIBLE);
            progressLoader.setVisibility(View.VISIBLE);

            Glide.with(this)
                    .load(preguntaActual.imagen)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .listener(new RequestListener<Drawable>() {
                        @Override
                        public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                            progressLoader.setVisibility(View.GONE);
                            iniciarReloj(); // Iniciamos aunque falle la imagen para no bloquear el juego
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

    // --- LÓGICA DE JUEGO ---

    /* Validacion de respuestas
    Al pulsar una opcion se oculta el boton de "Plantarse"
    Esto evita que se pueda pulsar mientras se valida y se carga la siguiente pregunta
     */
    private void validarRespuesta(int seleccionado) {
        if (reloj != null) reloj.cancel();
        btnPlantarse.setVisibility(View.INVISIBLE);

        if (seleccionado == preguntaActual.correcta) {
            // Acierto: Color Verde
            btnOpciones[seleccionado].setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#4CAF50")));

            if (nivelActual == 15) { // ¡Ha ganado el millón!
                new Handler().postDelayed(() -> irAResultados(15), 1500);
                return;
            }

            // Actualizamos progreso visual
            pbProgreso.setProgress(nivelActual);
            tvPremioActual.setText("NIVEL " + nivelActual + " > " + escalaPremios[nivelActual] + " €");

            nivelActual++;
            new Handler().postDelayed(this::mostrarTransicionYNivel, 1500);
        } else {
            // Fallo: Color Rojo y sumamos un fallo
            btnOpciones[seleccionado].setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#F44336")));
            actualizarFallos();
        }
    }

    /* Metodo tipo cortina para ganar tiempo en la carga de imagen de la siguiente pregunta
    Aprovechamos para mostrar un mensaje de nivel alcanzado y premio acumulado
    Al final se oculta el layout de transicion con una animacion
     */
    private void mostrarTransicionYNivel() {
        tvTransicionTitulo.setText("NIVEL " + nivelActual);
        tvTransicionPremio.setText(escalaPremios[nivelActual] + "€");

        if (nivelActual == 6 || nivelActual == 11) {
            tvTransicionMensajeLinea1.setTextColor(COLOR_AMBAR);
            tvTransicionMensajeLinea1.setText("¡ZONA SEGURA LOGRADA!");
            tvTransicionMensajeLinea2.setTextColor(Color.WHITE);
            tvTransicionMensajeLinea2.setText("Juegas por:");
        } else {
            tvTransicionMensajeLinea1.setText("Juegas por:");
            tvTransicionMensajeLinea1.setTextColor(Color.WHITE);
            tvTransicionMensajeLinea2.setVisibility(View.GONE);
        }

        layoutTransicion.setVisibility(View.VISIBLE);
        layoutTransicion.setAlpha(1.0f);

        cargarNivelCompleto(); // Recargamos preguntas del nuevo nivel

        // Animación de salida de la cortina (ocultar)
        new Handler().postDelayed(() -> {
            layoutTransicion.animate().alpha(0.0f).setDuration(500).withEndAction(() -> {
                layoutTransicion.setVisibility(View.GONE);
                mostrarSiguientePregunta();
            });
        }, 2000);
    }

    // Metodo que controla el crono de cuenta atrás (30s)
    private void iniciarReloj() {
        if (reloj != null) reloj.cancel();
        reloj = new CountDownTimer(30000, 1000) {
            public void onTick(long millisUntilFinished) {
                tvCronometro.setText("" + millisUntilFinished / 1000);
                if (millisUntilFinished < 6000) tvCronometro.setTextColor(Color.RED);
                else tvCronometro.setTextColor(Color.WHITE);
            }

            public void onFinish() {
                actualizarFallos();
                if (contadorFallos < 3) {
                    new Handler().postDelayed(() -> {
                        indicePregunta++;
                        prepararDatosPregunta();
                        mostrarSiguientePregunta();
                    }, 1500);
                }
            }
        }.start();
    }

    // Metodo que controla el contador de fallos
    private void actualizarFallos() {
        contadorFallos++;
        tvFallos.setText("Fallos: " + contadorFallos + "/3");
        if (contadorFallos >= 3) {
            if (reloj != null) reloj.cancel();
            // Si agota los fallos, se lleva el último "seguro" alcanzado (nivel 5 o 10)
            int premioSeguro = (nivelActual > 10) ? 10 : (nivelActual > 5 ? 5 : 0);
            irAResultados(premioSeguro);
        }
    }

    // --- COMODINES ---

    /* Metodo del comodin del publico
    Apaga el boton de las opciones descartadas
    Resalta las opciones recomendadas en ambar obtenidas de la bbdd
    A su vez, llama al metodo que apaga el boton del comodin
    */
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

    /* Metodo del comodin del publico
    Resalta la opcion recomendada con un color distinto al resto de comodines
    Al usarse muestra abajo un toaster con la recomendacion
    A su vez, llama al metodo que apaga el boton del comodin
    */
    private void comodinPublico() {
        // Si ya se usó, no hay pregunta o hay otro efecto en marcha, salimos
        if (usadoPublico || preguntaActual == null || efectoActivo) return;

        usadoPublico = true;
        efectoActivo = true; // Bloqueamos otros comodines temporalmente
        desactivaBotonComodin(btnPublico);

        //  Generar porcentajes teniendo en cuenta si el 50% se utilizó anteriormente y eliminó opciones
        int correcta = preguntaActual.comodin_publico;
        int[] porcentajes = generarVotosInteligentes(correcta);

        // Aplicar "Mapa de Calor" y textos temporales
        for (int i = 0; i < 4; i++) {
            // Solo afectamos a los botones que no han sido eliminados por el 50%
            if (btnOpciones[i].isEnabled()) {
                String enunciadoOriginal = preguntaActual.opciones.get(i);
                btnOpciones[i].setText(enunciadoOriginal + " (" + porcentajes[i] + "%)");
                btnOpciones[i].setBackgroundTintList(ColorStateList.valueOf(obtenerColor(porcentajes[i])));
            }
        }

        Toast.makeText(this, "Consultando al público...", Toast.LENGTH_SHORT).show();

        // 3. Temporizador para revertir los cambios
        new Handler().postDelayed(() -> {
            for (int i = 0; i < 4; i++) {
                // Restauramos el texto original (siempre)
                btnOpciones[i].setText(preguntaActual.opciones.get(i));

                // Si el botón está habilitado, le quitamos el color de calor
                if (btnOpciones[i].isEnabled()) {
                    btnOpciones[i].setBackgroundTintList(ColorStateList.valueOf(Color.TRANSPARENT));
                }
            }
            efectoActivo = false; // Liberamos el bloqueo
        }, 5000); // 5 segundos de espera
    }

    /**
     * Genera votos coherentes. Si el 50% ocultó opciones, esas reciben 0%.
     */
    private int[] generarVotosInteligentes(int indiceCorrecto) {
        int[] votos = new int[4];
        List<Integer> indicesActivos = new ArrayList<>();

        // Detectamos qué botones están "vivos" (no eliminados por el 50%)
        for (int i = 0; i < 4; i++) {
            if (btnOpciones[i].isEnabled()) {
                indicesActivos.size();
                indicesActivos.add(i);
            }
        }

        // Caso: Solo quedan 2 opciones (50% usado)
        if (indicesActivos.size() == 2) {
            votos[indiceCorrecto] = (int) (Math.random() * 16) + 75; // 75% a 90%
            for (int idx : indicesActivos) {
                if (idx != indiceCorrecto) votos[idx] = 100 - votos[indiceCorrecto];
            }
        }
        // Caso: Quedan las 4 opciones
        else {
            votos[indiceCorrecto] = (int) (Math.random() * 21) + 50; // 50% a 70%
            int restante = 100 - votos[indiceCorrecto];
            for (int i = 0; i < indicesActivos.size(); i++) {
                int currentIdx = indicesActivos.get(i);
                if (currentIdx == indiceCorrecto) continue;

                // Reparto aleatorio para el resto
                int randomVoto = (int) (Math.random() * (restante / 1.5));
                votos[currentIdx] = randomVoto;
                restante -= randomVoto;
            }
            // Ajuste final para sumar 100 exactamente
            for (int idx : indicesActivos) {
                if (idx != indiceCorrecto) { votos[idx] += restante; break; }
            }
        }
        return votos;
    }

    /**
     * Escala de colores para el feedback visual
     */
    private int obtenerColor(int porcentaje) {
        if (porcentaje >= 75) return Color.parseColor("#880E4F"); // Magenta muy oscuro
        if (porcentaje >= 50) return Color.parseColor("#C2185B"); // Magenta intenso
        if (porcentaje >= 25) return Color.parseColor("#E91E63"); // Magenta medio
        if (porcentaje >= 10) return Color.parseColor("#F06292"); // Magenta suave
        return Color.parseColor("#BDBDBD"); // Gris para opciones casi sin votos
    }

    /* Metodo del comodin de la llamada
    Resalta la opcion recomendada con un color distinto al resto de comodines
    Al usarse muestra abajo un toaster con la recomendacion
    A su vez, llama al metodo que apaga el boton del comodin
     */
    private void comodinLlamada() {
        if (usadoLlamada || preguntaActual == null) return;
        usadoLlamada = true;
        desactivaBotonComodin(btnLlamada);

        int sug = preguntaActual.comodin_llamada;
        btnOpciones[sug].setBackgroundTintList(ColorStateList.valueOf(COLOR_CYAN));
        Toast.makeText(this, "Tu contacto cree que es la " + (sug + 1), Toast.LENGTH_SHORT).show();
    }

    // --- MÉTODOS AUXILIARES ---

    // Metodo que apaga los comodines al usarlos
    private void desactivaBotonComodin(View v) {
        v.setEnabled(false);
        v.setAlpha(0.3f);
    }

    // Metodo intent para la ResultadoActivity
    private void irAResultados(int nivel) {
        Intent intent = new Intent(this, ResultadoActivity.class);
        intent.putExtra("NIVEL_ALCANZADO", nivel);
        startActivity(intent);
        finish();
    }

    // Al pulsar "Plantarse" pedimos confirmación
    private void mensajePlantarse() {
        int dinero = (nivelActual > 1) ? escalaPremios[nivelActual - 1] : 0;
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("¿Te plantas?")
                .setMessage("Te llevarías " + dinero + " € a casa.")
                .setPositiveButton("Sí, me planto", (d, w) -> irAResultados(nivelActual - 1))
                .setNegativeButton("Seguir jugando", null)
                .show();
    }

    // Al pulsar "Atras" tamiben pedimos confirmación
    private void mensajeAbandonar() {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Salir")
                .setMessage("Si sales ahora, se perderá todo el progreso.")
                .setPositiveButton("Salir", (d, w) -> finish())
                .setNegativeButton("Cancelar", null)
                .show();
    }
}