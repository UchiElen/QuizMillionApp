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
        // Acceso a la subcolección de Rubén
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

        PdfDocument pdf = new PdfDocument();
        PdfDocument.PageInfo info = new PdfDocument.PageInfo.Builder(595, 842, 1).create();
        PdfDocument.Page pagina = pdf.startPage(info);
        Canvas canvas = pagina.getCanvas();
        Paint paint = new Paint();

        // Cabecera
        paint.setTextSize(18f);
        paint.setFakeBoldText(true);
        canvas.drawText("HISTORIAL DE PUNTUACIONES - QUIZ APP", 50, 50, paint);

        paint.setTextSize(12f);
        paint.setFakeBoldText(false);
        int y = 100;

        // Tabla sencilla
        canvas.drawText("FECHA", 50, y, paint);
        canvas.drawText("PUNTOS", 200, y, paint);
        canvas.drawText("NIVEL", 350, y, paint);
        y += 20;
        canvas.drawLine(50, y, 500, y, paint);
        y += 30;

        for (Match m : matchHistory) {
            canvas.drawText(m.getFechaFormateada(), 50, y, paint);
            canvas.drawText(m.getScore() + " €", 200, y, paint);
            canvas.drawText("Pregunta " + m.getLevelReached(), 350, y, paint);
            y += 25;
            if (y > 800) break; // Límite de página simple
        }

        pdf.finishPage(pagina);
        guardarPdf(pdf);
    }

    private void guardarPdf(PdfDocument pdf) {
        // Crear el documento
        pdf = new PdfDocument();

        // Configurar la página
        android.graphics.pdf.PdfDocument.PageInfo info = new android.graphics.pdf.PdfDocument.PageInfo.Builder(595, 842, 1).create();
        android.graphics.pdf.PdfDocument.Page pagina = pdf.startPage(info);

        // btener el Canvas de la página y crear el Paint
        android.graphics.Canvas canvas = pagina.getCanvas();
        android.graphics.Paint paint = new android.graphics.Paint();

        ContentValues cv = new ContentValues();
        paint.setColor(android.graphics.Color.parseColor("#753192")); // Tu colorPrimary
        canvas.drawText("QUIZ MILLION - HISTORIAL", 50, 50, paint);
        cv.put(MediaStore.MediaColumns.DISPLAY_NAME, "MisPuntuaciones.pdf");
        cv.put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf");
        cv.put(MediaStore.MediaColumns.RELATIVE_PATH, "Download/QuizApp");

        Uri uri = getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, cv);
        try (OutputStream out = getContentResolver().openOutputStream(uri)) {
            pdf.writeTo(out);
            Toast.makeText(this, "PDF guardado en Descargas/QuizApp", Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            pdf.close();
        }
    }
}