package com.dam.quizmillionapp;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;

public class ResetActivity extends BaseActivity {
    private EditText emailTI;

    private Button RecupBtn;

    FirebaseAuth fAuth;
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_reset);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        emailTI = findViewById(R.id.emailTI);
        RecupBtn = findViewById(R.id.RecupBtn);
        fAuth = FirebaseAuth.getInstance();

        RecupBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SoundManager.getInstance(ResetActivity.this).playClick();

                // Validamos conexión antes de enviar el email de recuperación
                if (!isConnected()) {
                    showNoInternetDialog();
                    return;
                }

                String email = emailTI.getText().toString().trim();

                if (TextUtils.isEmpty(email)) {
                    emailTI.setError("Se requiere un email.");
                    return;
                }
                fAuth.sendPasswordResetEmail(email).addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            Toast.makeText(ResetActivity.this, "Email enviado.", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(ResetActivity.this, "Error !" + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }
        });

    }




}