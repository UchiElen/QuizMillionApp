package com.dam.quizmillionapp;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;

public class AuthActivity extends BaseActivity {
    private EditText emailTI, contraTI;

    private TextView recupTV;
    private Button AuthBtn;
    private ProgressBar progressBar;
    FirebaseAuth fAuth;
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_auth);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        emailTI = findViewById(R.id.emailTI);
        contraTI = findViewById(R.id.contraTI);
        AuthBtn = findViewById(R.id.AuthBtn);
        progressBar = findViewById(R.id.progressBar2);
        fAuth = FirebaseAuth.getInstance();
        recupTV = findViewById(R.id.recupTV);

        AuthBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String email = emailTI.getText().toString().trim();
                String contrasena = contraTI.getText().toString().trim();

                if (TextUtils.isEmpty(email)) {
                    emailTI.setError("Se requiere un email.");
                    return;
                }

                if (TextUtils.isEmpty(contrasena)) {
                    contraTI.setError("Se requiere una contraseña.");
                    return;
                }

                if (contrasena.length() < 6) {
                    contraTI.setError("La contraseña debe tener al menos 6 caracteres.");
                    return;
                }

                progressBar.setVisibility(View.VISIBLE);


                fAuth.signInWithEmailAndPassword(email, contrasena).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            Toast.makeText(AuthActivity.this, "Sesión iniciada correctamente.", Toast.LENGTH_SHORT).show();
                            startActivity(new Intent(getApplicationContext(), MainActivity.class));

                        } else {
                            progressBar.setVisibility(View.GONE);

                            try {
                                throw task.getException();
                            } catch (com.google.firebase.auth.FirebaseAuthInvalidUserException e) {
                                Toast.makeText(AuthActivity.this, "El email no está registrado.", Toast.LENGTH_SHORT).show();
                            } catch (
                                    com.google.firebase.auth.FirebaseAuthInvalidCredentialsException e) {
                                Toast.makeText(AuthActivity.this, "Contraseña o email incorrectos.", Toast.LENGTH_SHORT).show();
                            } catch (Exception e) {

                                Toast.makeText(AuthActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        }
                    }
                });
            }
        });

        recupTV.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(AuthActivity.this,ResetActivity.class);
                startActivity(intent);
            }
        });
    }
}