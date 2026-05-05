package com.dam.quizmillionapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;

public class WelcomeActivity extends BaseActivity {

    FirebaseAuth fAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_welcome);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Validamos conexión antes de comprobar la sesión de Firebase
        if (!isConnected()) {
            showNoInternetDialog();
            return;
        }

        fAuth = FirebaseAuth.getInstance();

        if (fAuth.getCurrentUser() != null) {
            String emailLogueado = fAuth.getCurrentUser().getEmail();
            Toast.makeText(WelcomeActivity.this, "Usuario ya logueado con: " + emailLogueado, Toast.LENGTH_LONG).show();
            startActivity(new Intent(getApplicationContext(), MainActivity.class));
            finish();

        }

        Button button = findViewById(R.id.RegisBtn);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                // Validamos conexión antes de ir al registro
                if (!isConnected()) {
                    showNoInternetDialog();
                    return;
                }

                SoundManager.getInstance(WelcomeActivity.this).playClick();
                Intent intent = new Intent(WelcomeActivity.this,RegisActivity.class);
                startActivity(intent);
            }
        });

        Button button2 = findViewById(R.id.AuthBtn);
        button2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                // Validamos conexión antes de ir al inicio de sesión
                if (!isConnected()) {
                    showNoInternetDialog();
                    return;
                }


                SoundManager.getInstance(WelcomeActivity.this).playClick();
                Intent intent = new Intent(WelcomeActivity.this,AuthActivity.class);
                startActivity(intent);
            }
        });
    }
}