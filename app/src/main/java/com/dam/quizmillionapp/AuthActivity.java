package com.dam.quizmillionapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;

public class AuthActivity extends BaseActivity {
    private EditText emailTI, contraTI;
    private Button AuthBtn;
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
        fAuth = FirebaseAuth.getInstance();

        if (fAuth.getCurrentUser() != null) {
            String emailLogueado = fAuth.getCurrentUser().getEmail();
            Toast.makeText(AuthActivity.this, "Usuario ya logueado con: " + emailLogueado, Toast.LENGTH_LONG).show();
            startActivity(new Intent(getApplicationContext(), MainActivity.class));
            finish();

        }

        AuthBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if(validateData()){
                    login();
                }
            }
        });



    }

    private boolean validateData(){
        boolean status = false;

        if(emailTI.getText().toString().isEmpty()){
            emailTI.setError("Introduce email");
            return false;
        }

        if(contraTI.getText().toString().isEmpty()){
            contraTI.setError("Introduce contraseña");
            return false;
        }



        return status;
    }

    private void login(){

    }
}