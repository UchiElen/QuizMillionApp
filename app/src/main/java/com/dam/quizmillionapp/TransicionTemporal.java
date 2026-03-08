package com.dam.quizmillionapp;

import android.content.Intent;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class TransicionTemporal extends AppCompatActivity {
     @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_transicion_temporal);
            EdgeToEdge.enable(this);

            findViewById(R.id.btnIrAlJuego).setOnClickListener(v -> {
                startActivity(new Intent(TransicionTemporal.this, PreguntasActivity.class));
            });
        }
    }
