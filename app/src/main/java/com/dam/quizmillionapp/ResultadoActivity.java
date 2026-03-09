package com.dam.quizmillionapp;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class ResultadoActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_resultado);

        TextView tvBanner = findViewById(R.id.tv_banner_mensaje);
        ImageView imgCromo = findViewById(R.id.img_cromo_premio);
        int premio = getIntent().getIntExtra("PREMIO", 0);

        // Lógica según tus diseños
        if (premio == 0) {
            tvBanner.setText("¡Qué mal!"); // [cite: 90]
            tvBanner.setBackgroundColor(Color.parseColor("#D32F2F")); // Rojo reproche
            imgCromo.setImageResource(R.drawable.cromo_0);
        } else {
            tvBanner.setText("¡Enhorabuena!"); // [cite: 111, 119]
            tvBanner.setBackgroundColor(Color.parseColor("#00C853")); // Verde felicitación

            // Cambiamos el cromo según el monto del PDF
            switch (premio) {
                case 1500: imgCromo.setImageResource(R.drawable.cromo_1500); break; // [cite: 113]
                case 2500: imgCromo.setImageResource(R.drawable.cromo_2500); break; // [cite: 121]
                case 1000000: imgCromo.setImageResource(R.drawable.cromo_millon); break; // [cite: 100, 175]
                default: imgCromo.setImageResource(R.drawable.cromo_generico); break;
            }
        }

        findViewById(R.id.btn_menu_principal).setOnClickListener(v -> finish());
    }
}