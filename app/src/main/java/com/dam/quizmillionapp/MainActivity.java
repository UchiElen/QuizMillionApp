package com.dam.quizmillionapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;

import androidx.activity.EdgeToEdge;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.bumptech.glide.Glide;
import com.dam.quizmillionapp.activities.LobbyActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

public class MainActivity extends BaseActivity {

    private FirebaseAuth fAuth;
    private FirebaseStorage storage;
    private StorageReference storageReference;
    private String userID;
    private ImageButton perfilIB;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        setContentView(R.layout.activity_main);

        fAuth = FirebaseAuth.getInstance();
        storage = FirebaseStorage.getInstance();
        storageReference = storage.getReference();
        perfilIB = findViewById(R.id.perfilIB);

        if (fAuth.getCurrentUser() != null) {
            userID = fAuth.getCurrentUser().getUid();
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        Button buttonMulti = findViewById(R.id.jugarMultiBtn);
        buttonMulti.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SoundManager.getInstance(MainActivity.this).playClick();
                Intent intent = new Intent(MainActivity.this, LobbyActivity.class);
                startActivity(intent);
            }
        });

        Button buttonSolitario = findViewById(R.id.jugarSoloBtn);
        buttonSolitario.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SoundManager.getInstance(MainActivity.this).playClick();
                Intent intent = new Intent(MainActivity.this, PreguntasActivity.class);
                startActivity(intent);
            }
        });

        Button buttonRecords = findViewById(R.id.recordsBtn);
        buttonRecords.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SoundManager.getInstance(MainActivity.this).playClick();
                Intent intent = new Intent(MainActivity.this, RecordsUsuarioActivity.class);
                startActivity(intent);
            }
        });

        perfilIB.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SoundManager.getInstance(MainActivity.this).playClick();
                Intent intent = new Intent(MainActivity.this,PerfilActivity.class);
                startActivity(intent);
            }
        });

        Button button2 = findViewById(R.id.configBtn);
        button2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SoundManager.getInstance(MainActivity.this).playClick();
                Intent intent = new Intent(MainActivity.this,ConfiguracionActivity.class);
                startActivity(intent);
            }
        });

        Button button3 = findViewById(R.id.creditosBtn);
        button3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SoundManager.getInstance(MainActivity.this).playClick();
                Intent intent = new Intent(MainActivity.this,CreditosActivity.class);
                startActivity(intent);
            }
        });

        if (userID != null) {
            cargarFotoPerfil();
        }

    }

    private void cargarFotoPerfil() {
        StorageReference profileRef = storageReference.child("users/" + userID + "/profile.jpg");

        profileRef.getDownloadUrl().addOnSuccessListener(uri -> {
            Glide.with(this)
                    .load(uri)
                    .placeholder(R.drawable.mascot) // Imagen mientras carga
                    .error(R.drawable.mascot)       // Imagen si falla
                    .circleCrop()                  // La hace circular
                    .diskCacheStrategy(com.bumptech.glide.load.engine.DiskCacheStrategy.NONE)
                    .skipMemoryCache(true)
                    .into(perfilIB);                // Se mete en el perfilIB
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (userID != null) {
            cargarFotoPerfil();
        }
    }
}