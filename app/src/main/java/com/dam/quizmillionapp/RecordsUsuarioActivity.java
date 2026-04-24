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
        // Acceso a la subcolección
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
            Toast.makeText(this, "No hay partidas para exportar", Toast.LENGTH_SHORT).show();
            return;
        }
        // 1. Cargar el logo desde los recursos y escalarlo
        android.graphics.Bitmap bitmap = android.graphics.BitmapFactory.decodeResource(getResources(), R.drawable.padded_logo_quizmillion);
        // Lo escalamos para que no ocupe toda la página (por ejemplo, 80x80 dp)
        android.graphics.Bitmap logoEscalado = android.graphics.Bitmap.createScaledBitmap(bitmap, 80, 80, false);

        Toast.makeText(this, "Generando reporte...", Toast.LENGTH_SHORT).show();

        // 1. Obtener datos necesarios que faltaban
        String nombreUsuario = FirebaseAuth.getInstance().getCurrentUser().getDisplayName();
        if (nombreUsuario == null || nombreUsuario.isEmpty()) {
            nombreUsuario = FirebaseAuth.getInstance().getCurrentUser().getEmail();
        }

        java.text.SimpleDateFormat sdfHoy = new java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault());
        String fechaHoy = sdfHoy.format(new java.util.Date());

        // 2. Inicializar PDF y herramientas de dibujo
        PdfDocument pdf = new PdfDocument();
        PdfDocument.PageInfo info = new PdfDocument.PageInfo.Builder(595, 842, 1).create();
        PdfDocument.Page pagina = pdf.startPage(info);
        Canvas canvas = pagina.getCanvas();
        Paint paint = new Paint();

        // --- CABECERA CON LOGO ---
        // Dibujamos el logo en la posición X=50, Y=20
        canvas.drawBitmap(logoEscalado, 50, 20, paint);

        // Ajustamos el título para que aparezca al lado del logo (X=150)
        paint.setColor(android.graphics.Color.parseColor("#753192"));
        paint.setTextSize(22f);
        paint.setFakeBoldText(true);
        canvas.drawText("QUIZMILLIONAPP", 150, 60, paint);
        paint.setTextSize(15f);
        paint.setFakeBoldText(false);
        canvas.drawText("HISTORIAL DE PUNTUACIONES", 150, 85, paint);

        // Los datos del jugador los bajamos un poco para que no choquen con el logo grande
        paint.setColor(android.graphics.Color.BLACK);
        paint.setTextSize(14f);
        paint.setFakeBoldText(false);
        int yCabecera = 120;
        canvas.drawText("Jugador: " + nombreUsuario, 50, yCabecera, paint);
        canvas.drawText("Fecha generación: " + fechaHoy, 350, yCabecera, paint);

        // La tabla empezará ahora en 170
        int yActual = 170;

        // --- TABLA (Encabezados) ---
        yActual = 150; // Variable de control vertical
        paint.setTextSize(12f);
        paint.setFakeBoldText(true);
        canvas.drawText("FECHA", 40, yActual, paint);
        canvas.drawText("SALA", 150, yActual, paint);      // Nueva columna para roomName
        canvas.drawText("MODO", 280, yActual, paint);      // Nueva columna para mode
        canvas.drawText("PREMIO", 380, yActual, paint);
        canvas.drawText("NIVEL", 480, yActual, paint);

        paint.setStrokeWidth(1f);
        canvas.drawLine(40, yActual + 10, 560, yActual + 10, paint);

        // --- LISTADO DE PARTIDAS ---
        paint.setFakeBoldText(false);
        yActual += 40;

        for (Match m : matchHistory) {
            canvas.drawText(m.getFechaFormateada(), 40, yActual, paint);

            // Controlar que el nombre de la sala no sea demasiado largo
            String sala = m.getRoomName();
            if (sala.length() > 18) sala = sala.substring(0, 15) + "...";
            canvas.drawText(sala, 150, yActual, paint);

            canvas.drawText(m.getMode(), 280, yActual, paint);
            canvas.drawText(m.getScore() + " €", 380, yActual, paint);
            canvas.drawText("Nivel " + m.getLevelReached(), 480, yActual, paint);

            yActual += 25;
            if (yActual > 800) break;
        }

        // 3. Finalizar y Guardar
        pdf.finishPage(pagina);
        guardarPdf(pdf);
        logoEscalado.recycle();
    }

    private void guardarPdf(PdfDocument pdf) {
        java.text.SimpleDateFormat sdfNombre = new java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.getDefault());
        String timestamp = sdfNombre.format(new java.util.Date());
        String nombreArchivo = "Historial_QuizMillionApp_" + timestamp + ".pdf";

        ContentValues cv = new ContentValues();
        cv.put(MediaStore.MediaColumns.DISPLAY_NAME, nombreArchivo);
        cv.put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf");
        cv.put(MediaStore.MediaColumns.RELATIVE_PATH, "Download/QuizMillionApp");
        // Esta línea es clave para que el sistema lo registre formalmente
        cv.put(MediaStore.Downloads.IS_PENDING, 1);

        Uri uri = getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, cv);

        try (OutputStream out = getContentResolver().openOutputStream(uri)) {
            if (out != null) {
                pdf.writeTo(out);

                // Liberamos el archivo para que el sistema lo vea terminado
                cv.clear();
                cv.put(MediaStore.Downloads.IS_PENDING, 0);
                getContentResolver().update(uri, cv, null, null);

                Toast.makeText(this, "✅ Informe disponible en Descargas/QuizMillionApp", Toast.LENGTH_LONG).show();

                // OPCIONAL: Escanear el archivo para que aparezca la notificación de "Descarga completada"
                android.media.MediaScannerConnection.scanFile(this,
                        new String[]{ android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS) + "/QuizMillionApp/" + nombreArchivo },
                        new String[]{"application/pdf"}, null);
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "❌ Error al guardar: " + e.getMessage(), Toast.LENGTH_LONG).show();
        } finally {
            pdf.close();
        }
    }
}