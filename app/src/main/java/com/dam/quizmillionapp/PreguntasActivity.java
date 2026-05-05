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

    private Handler handlerGlobal = new Handler() ;

    // --- Layouts de Transición (Cortinas) ---
    private androidx.constraintlayout.widget.ConstraintLayout layoutTransicion;
    private TextView tvTransicionTitulo, tvTransicionMensajeLinea1,tvTransicionMensajeLinea2, tvTransicionPremio;

    // --- Lógica de Juego y Datos ---
    private FirebaseFirestore db;
    private List<Pregunta> listaPreguntasNivel = new ArrayList<>();
    private Pregunta preguntaActual;
    private CountDownTimer reloj;
    private String roomId;

    private boolean modoSolitario = false;

    private int contadorFallos = 0;
    private int nivelActual = 1;
    private int indicePregunta = 0;
    private boolean usado50 = false, usadoPublico = false, usadoLlamada = false;

    private boolean efectoActivo = false; // Bloqueo de seguridad para que no se pisen los efectos

    // --- Constantes de Diseño ---
    private final int[] escalaPremios = {0, 100, 250, 500, 750, 1500, 2500, 5000, 10000, 15000, 20000, 30000, 50000, 100000, 300000, 1000000};


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_preguntas);

        if (getIntent().hasExtra("roomId")) {
            roomId = getIntent().getStringExtra("roomId");
        }
        else {
            roomId = "";
            modoSolitario=true;
        }


        initViews();       // Enlazamos XML con Java
        initFirestore();    // Inicializamos DB

        // Validamos conexión antes de cargar preguntas desde Firebase
        if (!isConnected()) {
            showNoInternetDialog();
            return;
        }

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

        // Validamos conexión antes de consultar el nivel en Firestore
        if (!isConnected()) {
            showNoInternetDialog();
            cancelarTodo();
            return;
        }

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
        btnPlantarse.setAlpha(1.0f);
        btnPlantarse.setEnabled(true);
        tvEnunciado.setText(preguntaActual.enunciado);

        for (int i = 0; i < 4; i++) {
            btnOpciones[i].setVisibility(View.VISIBLE);
            btnOpciones[i].setEnabled(true);
            btnOpciones[i].setAlpha(1.0f);
            btnOpciones[i].setText(preguntaActual.opciones.get(i));

            int colorTransparente = androidx.core.content.ContextCompat.getColor(this, R.color.transparent);
            int colorBlanco = androidx.core.content.ContextCompat.getColor(this, R.color.white);

            btnOpciones[i].setBackgroundTintList(ColorStateList.valueOf(colorTransparente));
            btnOpciones[i].setStrokeColor(ColorStateList.valueOf(colorBlanco));
            btnOpciones[i].setTextColor(colorBlanco);
        }
        if (!usado50) { btn50.setEnabled(true); btn50.setAlpha(1.0f); }
        if (!usadoPublico) { btnPublico.setEnabled(true); btnPublico.setAlpha(1.0f); }
        if (!usadoLlamada) { btnLlamada.setEnabled(true); btnLlamada.setAlpha(1.0f); }
        btnPlantarse.setEnabled(true);
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

        SoundManager.getInstance(PreguntasActivity.this).playClick();

        if (seleccionado == preguntaActual.correcta) {
            // Acierto: Color Verde
            desactivarTodo();
            SoundManager.getInstance(PreguntasActivity.this).playSuccess();
            btnOpciones[seleccionado].setBackgroundTintList(androidx.core.content.ContextCompat.getColorStateList(this, R.color.green));

            if (nivelActual == 15) { // ¡Ha ganado el millón!
                handlerGlobal.postDelayed(() -> irAResultados(15), 1500);
                return;
            }

            // Actualizamos progreso visual
            pbProgreso.setProgress(nivelActual);
            tvPremioActual.setText("NIVEL " + nivelActual + " : " + escalaPremios[nivelActual] + " €");

            nivelActual++;
            handlerGlobal.postDelayed(this::mostrarTransicionYNivel, 1500);
        } else {
            // Fallo: Color Rojo y sumamos un fallo
            SoundManager.getInstance(PreguntasActivity.this).playError();
            btnOpciones[seleccionado].setBackgroundTintList(androidx.core.content.ContextCompat.getColorStateList(this, R.color.red));
            // deshabilitar SOLO el botón pulsado para que no se vuelva a marcar
            btnOpciones[seleccionado].setEnabled(false);
            btnOpciones[seleccionado].setAlpha(0.5f);
            actualizarFallos();
        }
        // Si despues de actualizar fallos aún le quedan intentos
        if (contadorFallos < 3) {
            // Devolver el botón de plantarse
            btnPlantarse.setVisibility(View.VISIBLE);
        } else {
            // si ya ha perdido (3 fallos), entonces si bloquear
            desactivarTodo();
        }

    }

    /* Metodo tipo cortina para ganar tiempo en la carga de imagen de la siguiente pregunta
    Aprovechamos para mostrar un mensaje de nivel alcanzado y premio acumulado
    Al final se oculta el layout de transicion con una animacion
     */
    private void mostrarTransicionYNivel() {
        tvTransicionTitulo.setText("NIVEL " + nivelActual);
        tvTransicionPremio.setText(escalaPremios[nivelActual] + "€");

        int colorBlanco = androidx.core.content.ContextCompat.getColor(this, R.color.white);
        int colorAmbar = androidx.core.content.ContextCompat.getColor(this, R.color.ambar_transition);

        if (nivelActual == 6 || nivelActual == 11) {
            tvTransicionMensajeLinea1.setTextColor(colorAmbar);
            tvTransicionMensajeLinea1.setText("¡ZONA SEGURA LOGRADA!");
            tvTransicionMensajeLinea2.setTextColor(colorBlanco);
            tvTransicionMensajeLinea2.setText("Juegas por:");
        } else {
            tvTransicionMensajeLinea1.setTextColor(colorBlanco);
            tvTransicionMensajeLinea1.setText("Juegas por:");
            tvTransicionMensajeLinea2.setVisibility(View.GONE);
        }

        layoutTransicion.setVisibility(View.VISIBLE);
        layoutTransicion.setAlpha(1.0f);

        cargarNivelCompleto(); // Recargamos preguntas del nuevo nivel

        // Animación de salida de la cortina (ocultar)
        handlerGlobal.postDelayed(() -> {
            layoutTransicion.animate().alpha(0.0f).setDuration(500).withEndAction(() -> {
                layoutTransicion.setVisibility(View.GONE);
                mostrarSiguientePregunta();
            });
        }, 2000);
    }

    // Metodo que controla el crono de cuenta atrás (30s)
    private void iniciarReloj() {
        if (reloj != null) {
            reloj.cancel();
            reloj = null;
        }
        reloj = new CountDownTimer(30000, 1000) {
            public void onTick(long millisUntilFinished) {
                tvCronometro.setText("" + millisUntilFinished / 1000);
                if (millisUntilFinished < 6000)
                    tvCronometro.setTextColor(androidx.core.content.ContextCompat.getColor(PreguntasActivity.this, R.color.red));
                else
                    tvCronometro.setTextColor(androidx.core.content.ContextCompat.getColor(PreguntasActivity.this, R.color.white));            }

            public void onFinish() {
                reloj = null;
                actualizarFallos();
                if (contadorFallos < 3) {
                    handlerGlobal.postDelayed(() -> {
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
        vibrarAlFallar();
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
        SoundManager.getInstance(PreguntasActivity.this).playClick();
        usado50 = true;
        desactivaBotonComodin(btn50);

        for (int i = 0; i < 4; i++) {
            if (preguntaActual.comodin_50.contains(i)) {
                btnOpciones[i].setBackgroundTintList(androidx.core.content.ContextCompat.getColorStateList(this, R.color.comodin_50));
                btnOpciones[i].setTextColor(androidx.core.content.ContextCompat.getColor(this, R.color.black));
            } else {
                btnOpciones[i].setEnabled(false);
                btnOpciones[i].setAlpha(0.3f);
            }
        }
    }

    /* Metodo del comodin del publico
    Resalta las opciones votadas disponibles con una escala de  color en funcion del porcentaje
    Al usarse muestra abajo un toaster con el mensaje "Consultando al público..."
    A su vez, llama al metodo que apaga el boton del comodin y bloquea los demás para evitar uso indevido
    */
    private void comodinPublico() {

        if (usadoPublico || preguntaActual == null || efectoActivo) return;
        SoundManager.getInstance(PreguntasActivity.this).playClick();

        usadoPublico = true;
        efectoActivo = true;
        desactivaBotonComodin(btnPublico);

        int correcta = preguntaActual.comodin_publico;
        int[] porcentajes = generarVotos(correcta);

        for (int i = 0; i < 4; i++) {
            if (btnOpciones[i].isEnabled()) {
                String enunciadoOriginal = preguntaActual.opciones.get(i);
                btnOpciones[i].setText(enunciadoOriginal + " (" + porcentajes[i] + "%)");
                btnOpciones[i].setBackgroundTintList(ColorStateList.valueOf(obtenerColor(porcentajes[i])));
                btnOpciones[i].setTextColor(androidx.core.content.ContextCompat.getColor(this, R.color.white)); // Aseguramos legibilidad
            }
        }

        Toast.makeText(this, "Consultando al público...", Toast.LENGTH_SHORT).show();

        handlerGlobal.postDelayed(() -> {
            for (int i = 0; i < 4; i++) {
                btnOpciones[i].setText(preguntaActual.opciones.get(i));
                if (btnOpciones[i].isEnabled()) {
                    btnOpciones[i].setBackgroundTintList(ColorStateList.valueOf(R.color.transparent));
                }
            }
            efectoActivo = false;
        }, 5000);
    }

    private int[] generarVotos(int indiceCorrecto) {
        int[] votos = new int[4];
        List<Integer> indicesActivos = new ArrayList<>();

        // Detectar qué botones de opciones están activos
        for (int i = 0; i < 4; i++) {
            if (btnOpciones[i].isEnabled()) indicesActivos.add(i);
        }

        int baseVoto, rangoVoto;

        // Lógica de dificultad por nivel para obtener un resultado mas real
        if (indicesActivos.size() == 2) {
            // --- CASO 50% USADO (Solo 2 opciones) ---
            if (nivelActual <= 5) { baseVoto = 85; rangoVoto = 11; }      // 85-95%
            else if (nivelActual > 5 && nivelActual <= 10) { baseVoto = 65; rangoVoto = 16; } // 65-80%
            else { baseVoto = 51; rangoVoto = 10; }                       // 51-60%
        } else {
            // --- CASO NORMAL (4 opciones) ---
            if (nivelActual <= 5) { baseVoto = 70; rangoVoto = 15; }      // 70-85%
            else if (nivelActual > 5 && nivelActual <= 10) { baseVoto = 50; rangoVoto = 15; } // 50-65%
            else { baseVoto = 35; rangoVoto = 12; }                       // 35-47%
        }

        // Asignar voto a la correcta
        votos[indiceCorrecto] = (int) (Math.random() * rangoVoto) + baseVoto;
        int restante = 100 - votos[indiceCorrecto];

        // Repartir el resto entre las incorrectas habilitadas
        List<Integer> incorrectasActivas = new ArrayList<>();
        for (int idx : indicesActivos) {
            if (idx != indiceCorrecto) incorrectasActivas.add(idx);
        }

        for (int i = 0; i < incorrectasActivas.size(); i++) {
            int currentIdx = incorrectasActivas.get(i);
            if (i == incorrectasActivas.size() - 1) {
                votos[currentIdx] = restante; // La última se queda el resto exacto
            } else {
                // Reparto proporcional para que no sea siempre igual
                int randomVoto = (int) (Math.random() * (restante / 1.2));
                votos[currentIdx] = randomVoto;
                restante -= randomVoto;
            }
        }
        return votos;
    }

    /**
     * Escala de colores para el resultado visual
     */
    private int obtenerColor(int porcentaje) {
        if (porcentaje >= 75) return androidx.core.content.ContextCompat.getColor(this, R.color.comodin_publico_1); // Magenta muy oscuro
        if (porcentaje >= 50) return androidx.core.content.ContextCompat.getColor(this, R.color.comodin_publico_2); // Magenta intenso
        if (porcentaje >= 25) return androidx.core.content.ContextCompat.getColor(this, R.color.comodin_publico_3); // Magenta medio
        if (porcentaje >= 10) return androidx.core.content.ContextCompat.getColor(this, R.color.comodin_publico_4); // Magenta suave
        return androidx.core.content.ContextCompat.getColor(this, R.color.gray); // Gris para opciones casi sin votos
    }

    /* Metodo del comodin de la llamada
    Resalta la opcion recomendada con un color distinto al resto de comodines
    Al usarse muestra abajo un toaster con la recomendacion
    A su vez, llama al metodo que apaga el boton del comodin
     */
    private void comodinLlamada() {
        if (usadoLlamada || preguntaActual == null) return;
        SoundManager.getInstance(PreguntasActivity.this).playClick();
        usadoLlamada = true;
        desactivaBotonComodin(btnLlamada);

        int sug = preguntaActual.comodin_llamada;
        btnOpciones[sug].setBackgroundTintList(androidx.core.content.ContextCompat.getColorStateList(this, R.color.comodin_llamada));
        btnOpciones[sug].setTextColor(androidx.core.content.ContextCompat.getColor(this, R.color.white));
        Toast.makeText(this, "Tu contacto ha decidido ", Toast.LENGTH_SHORT).show();
    }

    // --- MÉTODOS AUXILIARES ---

    // Metodo que apaga los comodines al usarlos
    private void desactivaBotonComodin(View v) {
        v.setEnabled(false);
        v.setAlpha(0.3f);
    }

    /* Metodo Intent para ir a Resultados
    Se usa la variable modoSolitario y la cadena roomId
    Para determinar si ir a ResultadosSolitario o ResultadosMultijugador
    */
    private void irAResultados(int nivel) {
        if (!roomId.isEmpty()) {
            Intent intent = new Intent(this, ResultadoMultiActivity.class);
            intent.putExtra("NIVEL_ALCANZADO", nivel);
            intent.putExtra("roomId", this.roomId);
            startActivity(intent);
            finish();
        }
        else if (modoSolitario) {
            Intent intent = new Intent(this, ResultadoSolitarioActivity.class);
            intent.putExtra("NIVEL_ALCANZADO", nivel);
            startActivity(intent);
            finish();
        }

    }

    // Al pulsar "Plantarse" pedimos confirmación
    private void mensajePlantarse() {
        SoundManager.getInstance(PreguntasActivity.this).playClick();
        int dinero = (nivelActual > 1) ? escalaPremios[nivelActual - 1] : 0;
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("¿Te plantas?")
                .setMessage("Te llevarías " + dinero + " € a casa.")
                .setPositiveButton("Sí, me planto", (d, w) -> {
                    // para el reloj
                    if (reloj != null) {
                        reloj.cancel();
                    }
                    // limpiar el handler
                    handlerGlobal.removeCallbacksAndMessages(null);
                    // pantalla Resultados
                    irAResultados(nivelActual - 1);
                })
                .setNegativeButton("Seguir jugando", null)
                .show();
    }

    // Al pulsar "Atras" también pedimos confirmación
    private void mensajeAbandonar() {
        SoundManager.getInstance(PreguntasActivity.this).playClick();
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Salir")
                .setMessage("Si sales ahora, se perderá todo el progreso.")
                .setPositiveButton("Salir", (d, w) -> {
                    // parar el reloj
                    if (reloj != null) {
                        reloj.cancel();
                    }
                    // limpiar el handler
                    handlerGlobal.removeCallbacksAndMessages(null);
                   // volver al menu principal
                    Intent intent = new Intent(PreguntasActivity.this, MainActivity.class); // <-- Cambia MainActivity por el nombre de tu clase de menú
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                    startActivity(intent);

                    finish();
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }
    // Para hacer vibrar el móvil si la opción está activada en ajustes
    private void vibrarAlFallar() {
        android.content.SharedPreferences prefs = getSharedPreferences(ConfiguracionActivity.PREFS_NAME, MODE_PRIVATE);
        boolean vibracionActivada = prefs.getBoolean(ConfiguracionActivity.KEY_VIBRATION, true);

        if (vibracionActivada) {
            android.os.Vibrator vibrator = (android.os.Vibrator) getSystemService(VIBRATOR_SERVICE);
            if (vibrator != null) {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    vibrator.vibrate(android.os.VibrationEffect.createOneShot(200, android.os.VibrationEffect.DEFAULT_AMPLITUDE));
                } else {
                    vibrator.vibrate(200);
                }
            }
        }
    }

    private void desactivarTodo() {
        //Bloquear las opciones
        for (MaterialButton btn : btnOpciones) {
            btn.setEnabled(false);
        }

        // Bloquear comodines
        btn50.setEnabled(false);
        btnPublico.setEnabled(false);
        btnLlamada.setEnabled(false);

        // Ocultar boton de plantarse
        if (btnPlantarse != null) {
            btnPlantarse.clearAnimation();
            btnPlantarse.setEnabled(false);
            btnPlantarse.setVisibility(View.GONE);
            btnPlantarse.setAlpha(0f);
        }
    }

    private void cancelarTodo() {
        if (reloj != null) reloj.cancel();
        handlerGlobal.removeCallbacksAndMessages(null);
    }

    @Override
    protected void onDestroy() {
        cancelarTodo();
        super.onDestroy();
    }
}