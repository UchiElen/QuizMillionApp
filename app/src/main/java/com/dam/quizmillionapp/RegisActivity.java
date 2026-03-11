package com.dam.quizmillionapp;

import static android.content.ContentValues.TAG;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class RegisActivity extends BaseActivity {

    EditText nombreTI, emailTI, contraTI;
    Button AuthBtn;
    FirebaseAuth fAuth;
    ProgressBar progressBar;
    ImageButton fotoIB;
    FirebaseFirestore fStore;
    String userID;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        setContentView(R.layout.activity_regis);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        nombreTI = findViewById(R.id.nomTI);
        emailTI = findViewById(R.id.emailTI);
        contraTI = findViewById(R.id.contraTI);
        AuthBtn = findViewById(R.id.AuthBtn);
        fAuth = FirebaseAuth.getInstance();
        progressBar = findViewById(R.id.progressBar);
        fotoIB = findViewById(R.id.fotoIB);
        fStore = FirebaseFirestore.getInstance();



        if (fAuth.getCurrentUser() != null) {
            String emailLogueado = fAuth.getCurrentUser().getEmail();
            Toast.makeText(RegisActivity.this, "Usuario ya logueado con: " + emailLogueado, Toast.LENGTH_LONG).show();
            startActivity(new Intent(getApplicationContext(), MainActivity.class));
            finish();

        }

        AuthBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String email = emailTI.getText().toString().trim();
                String contrasena = contraTI.getText().toString().trim();
                String nombre = nombreTI.getText().toString().trim();

                if (TextUtils.isEmpty(nombre)) {
                    nombreTI.setError("Se requiere un email.");
                    return;
                }

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

                fAuth.createUserWithEmailAndPassword(email, contrasena).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            Toast.makeText(RegisActivity.this, "Usuario creado.", Toast.LENGTH_SHORT).show();
                            userID = fAuth.getCurrentUser().getUid();
                            DocumentReference documentReference = fStore.collection("usuarios").document(userID);
                            Map<String, Object> user = new HashMap<>();
                            user.put("nombreUsuario", nombre);
                            user.put("email", email);
                            documentReference.set(user).addOnSuccessListener(new OnSuccessListener<Void>() {
                                @Override
                                public void onSuccess(Void unused) {
                                    Log.d(TAG, "onSuccess: usuario creado para " + userID);
                                }
                            });
                            startActivity(new Intent(getApplicationContext(), MainActivity.class));

                        } else {
                            Toast.makeText(RegisActivity.this, "Error !" + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                            progressBar.setVisibility(View.GONE);
                        }
                    }
                });
            }
        });
    }
}
