package com.dam.quizmillionapp;

import android.content.ContentValues;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.Button;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.dam.quizmillionapp.adapters.MatchAdapter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * genera un documento en pdf con el historial de puntuaciones del jugador
 * incluye logo, datos del jugador y una tabla detallada de resultados
 */

public class RecordsUsuarioActivity extends BaseActivity {

    private RecyclerView rv;
    private MatchAdapter adapter;
    private List<Match> matchHistory = new ArrayList<>();
    private FirebaseFirestore db;
    private String currentUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_records_usuario);

        db = FirebaseFirestore.getInstance();
        currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        rv = findViewById(R.id.rvRecords);
        rv.setLayoutManager(new LinearLayoutManager(this));
        adapter = new MatchAdapter(matchHistory);
        rv.setAdapter(adapter);

        findViewById(R.id.btnDownloadPdf).setOnClickListener(v -> generarPdfCompleto());

        obtenerDatosDeFirebase();
    }

    private void obtenerDatosDeFirebase() {
        // acceso a la subcolección match_history de Firestore para traer los datos clave
        db.collection("usuarios").document(currentUserId)
                .collection("match_history")
                .orderBy("playedAt", Query.Direction.DESCENDING)
                .addSnapshotListener((value, error) -> {
                    if (value != null) {
                        matchHistory.clear();
                        matchHistory.addAll(value.toObjects(Match.class));
                        adapter.notifyDataSetChanged();
                    }
                });
    }

    private void generarPdfCompleto() {
        if (matchHistory.isEmpty()) {
            Toast.makeText(this, "No hay puntuaciones para mostrar", Toast.LENGTH_SHORT).show();
            return;
        }
        // cargar el logo de la app para pintarlo en el informe
        android.graphics.Bitmap bitmap = android.graphics.BitmapFactory.decodeResource(getResources(), R.drawable.padded_logo_quizmillion);
        // ajustar tamaño
        android.graphics.Bitmap logoEscalado = android.graphics.Bitmap.createScaledBitmap(bitmap, 80, 80, false);
        // mostrar informacion del proceso
        Toast.makeText(this, "Generando informe...", Toast.LENGTH_SHORT).show();

        // identificar al jugador
        String nombreUsuario = FirebaseAuth.getInstance().getCurrentUser().getDisplayName();
        if (nombreUsuario == null || nombreUsuario.isEmpty()) {
            nombreUsuario = FirebaseAuth.getInstance().getCurrentUser().getEmail();
        }
        // generar y formatear fecha de manera legible para mostrar en el informe
        java.text.SimpleDateFormat sdfHoy = new java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault());
        String fechaHoy = sdfHoy.format(new java.util.Date());

        // inicializar PDF y herramientas de dibujo (canvas y paint)
        PdfDocument pdf = new PdfDocument();
        PdfDocument.PageInfo info = new PdfDocument.PageInfo.Builder(595, 842, 1).create();
        PdfDocument.Page pagina = pdf.startPage(info);
        Canvas canvas = pagina.getCanvas();
        Paint paint = new Paint();

        // --- CABECERA CON LOGO ---
        // Dibujar el logo
        canvas.drawBitmap(logoEscalado, 50, 20, paint);

        // Ajustar el título y subtitulo para que aparezcan al lado del logo
        paint.setColor(android.graphics.Color.parseColor("#753192"));
        paint.setTextSize(22f);
        paint.setFakeBoldText(true);
        canvas.drawText("QUIZMILLIONAPP", 150, 60, paint);
        paint.setTextSize(15f);
        paint.setFakeBoldText(false);
        canvas.drawText("HISTORIAL DE PUNTUACIONES", 150, 85, paint);

        // bajar los datos del jugador para que no invadan los demas datos
        paint.setColor(android.graphics.Color.BLACK);
        paint.setTextSize(14f);
        paint.setFakeBoldText(false);
        int yCabecera = 120;
        canvas.drawText("Jugador: " + nombreUsuario, 50, yCabecera, paint);
        canvas.drawText("Fecha generación: " + fechaHoy, 350, yCabecera, paint);

        // posicionar el comienzo de la tabla
        int yActual = 170;

        // --- CABECERAS (campos de firestore) ---
        yActual = 150; // variable de control vertical
        paint.setTextSize(12f);
        paint.setFakeBoldText(true);
        canvas.drawText("FECHA", 40, yActual, paint);
        canvas.drawText("SALA", 150, yActual, paint);
        canvas.drawText("MODO", 280, yActual, paint);
        canvas.drawText("PREMIO", 380, yActual, paint);
        canvas.drawText("NIVEL", 480, yActual, paint);

        // linea de separacion
        paint.setStrokeWidth(1f);
        canvas.drawLine(40, yActual + 10, 560, yActual + 10, paint);

        // --- LISTADO DE PARTIDAS ---
        // iterador de resultados
        paint.setFakeBoldText(false);
        yActual += 40;

        for (Match m : matchHistory) {
            canvas.drawText(m.getFechaFormateada(), 40, yActual, paint);

            // controlar que el nombre de la sala no sea demasiado largo
            String sala = m.getRoomName();
            if (sala.length() > 18) sala = sala.substring(0, 15) + "...";
            canvas.drawText(sala, 150, yActual, paint);

            canvas.drawText(m.getMode(), 280, yActual, paint);
            canvas.drawText(m.getScore() + " €", 380, yActual, paint);
            canvas.drawText("Nivel " + m.getLevelReached(), 480, yActual, paint);

            yActual += 25;
            if (yActual > 800) break;
        }

        // finalizar, guardar y limpiar
        pdf.finishPage(pagina);
        guardarPdf(pdf);
        logoEscalado.recycle();
    }

    // en el siguiente metodo se hace uso de MediaStore por motivos de compatibilidad
    private void guardarPdf(PdfDocument pdf) {
        // generar nombre de archivo con formato de fecha y hora: yyyyMMdd_HHmmss
        java.text.SimpleDateFormat sdfNombre = new java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.getDefault());
        String timestamp = sdfNombre.format(new java.util.Date());
        String nombreArchivo = "Hist_Records_" + timestamp + ".pdf";

        // definir la carpeta de destino una sola vez para evitar errores de ruta
        String carpetaDestino = "Download/QuizMillionApp";

        ContentValues cv = new ContentValues();
        cv.put(MediaStore.MediaColumns.DISPLAY_NAME, nombreArchivo);
        cv.put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf");
        cv.put(MediaStore.MediaColumns.RELATIVE_PATH, carpetaDestino);
        cv.put(MediaStore.Downloads.IS_PENDING, 1);

        Uri uri = getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, cv);

        try (OutputStream out = getContentResolver().openOutputStream(uri)) {
            if (out != null) {
                pdf.writeTo(out);

                // confirmar que el archivo ya no está pendiente
                cv.clear();
                cv.put(MediaStore.Downloads.IS_PENDING, 0);
                getContentResolver().update(uri, cv, null, null);

                // mostrar resultado de la generacion
                Toast.makeText(this, "✅ Informe guardado en Descargas/QuizMillionApp", Toast.LENGTH_SHORT).show();

                // para abrir el visor de PDF hay que volver al hilo principal
                runOnUiThread(() -> {
                    if (uri != null) {
                        android.content.Intent intent = new android.content.Intent(android.content.Intent.ACTION_VIEW);

                        // setear los datos
                        intent.setDataAndType(uri, "application/pdf");

                        // permite a la otra app leer el archivo
                        intent.addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION);

                        // si ya hay un visor abierto, lo limpia para mostrar este
                        intent.addFlags(android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP);

                        // por si no hay visor de pdf instalado en el dispositivo
                        try {
                            startActivity(intent);
                        } catch (android.content.ActivityNotFoundException e) {
                            Toast.makeText(RecordsUsuarioActivity.this,
                                    "No hay visor de pdf instalado",
                                    Toast.LENGTH_LONG).show();
                        }
                    }
                });

                // notificar al sistema para que el archivo aparezca en la carpeta de Descargas
                android.media.MediaScannerConnection.scanFile(this,
                        new String[]{android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS).toString() + "/QuizMillionApp/" + nombreArchivo},
                        new String[]{"application/pdf"}, null);
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "❌ Error al guardar el informe", Toast.LENGTH_SHORT).show();
        } finally {
            // cerrar el documento para liberar recursos de memoria
            pdf.close();
        }
    }
}