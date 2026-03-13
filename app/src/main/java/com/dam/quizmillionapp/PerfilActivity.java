package com.dam.quizmillionapp;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.bumptech.glide.Glide;

public class PerfilActivity extends BaseActivity {

    TextInputEditText nombreTI, emailTI, contraTI;
    ImageButton fotoIB;
    FirebaseAuth fAuth;
    FirebaseFirestore fStore;
    StorageReference storageReference;
    String userID;
    Button ActualizarBtn;

    private ActivityResultLauncher<Intent> galleryLauncher;
    private ActivityResultLauncher<android.net.Uri> cameraLauncher;
    private Uri cameraUri, imagenSeleccionadaUri;

    private ProgressBar progressBar;


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_perfil);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        nombreTI = findViewById(R.id.nomTI);
        emailTI = findViewById(R.id.emailTI);
        contraTI = findViewById(R.id.contraTI);
        fotoIB = findViewById(R.id.fotoIB);
        progressBar = findViewById(R.id.progressBar);
        fAuth = FirebaseAuth.getInstance();
        fStore = FirebaseFirestore.getInstance();
        storageReference = FirebaseStorage.getInstance().getReference();

        if (fAuth.getCurrentUser() != null) {
            userID = fAuth.getCurrentUser().getUid();
            cargarDatosUsuario();
        }

        galleryLauncher = registerForActivityResult(
                new androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == android.app.Activity.RESULT_OK && result.getData() != null) {
                        imagenSeleccionadaUri = result.getData().getData();
                        Glide.with(this)
                                .load(imagenSeleccionadaUri)
                                .circleCrop()
                                .into(fotoIB);
                    }
                }
        );

        cameraLauncher = registerForActivityResult(
                new androidx.activity.result.contract.ActivityResultContracts.TakePicture(),
                success -> {
                    if (success && cameraUri != null) {
                        imagenSeleccionadaUri = cameraUri;
                        Glide.with(this)
                                .load(imagenSeleccionadaUri)
                                .circleCrop()
                                .into(fotoIB);
                    }
                }
        );

        fotoIB.setOnClickListener(v -> mostrarOpcionesFoto());

        ActualizarBtn = findViewById(R.id.GuardarBtn);

        ActualizarBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String nuevoNombre = nombreTI.getText().toString().trim();
                String nuevaContra = contraTI.getText().toString().trim();


                if (nuevoNombre.isEmpty()) {
                    nombreTI.setError("El nombre de usuario no puede estar vacío.");
                    nombreTI.requestFocus();
                    return;
                }

                if (nuevoNombre.length() > 20) {
                    nombreTI.setError("El nombre no puede superar los 20 caracteres.");
                    nombreTI.requestFocus();
                    return;
                }



                progressBar.setVisibility(View.VISIBLE);
                ActualizarBtn.setEnabled(false);

                fStore.collection("usuarios").document(userID).update("nombreUsuario", nuevoNombre)
                        .addOnSuccessListener(aVoid -> {
                            if (imagenSeleccionadaUri != null) {
                                uploadImageAndFinish(imagenSeleccionadaUri);
                            } else {
                                progressBar.setVisibility(View.GONE);
                                Toast.makeText(PerfilActivity.this, "Datos actualizados", Toast.LENGTH_SHORT).show();
                                finish();
                            }
                        })
                        .addOnFailureListener(e -> {
                            progressBar.setVisibility(View.GONE);
                            ActualizarBtn.setEnabled(true);
                            Toast.makeText(PerfilActivity.this, "Error al actualizar datos", Toast.LENGTH_SHORT).show();
                        });

                if (!nuevaContra.isEmpty()) {
                    if (nuevaContra.length() < 6) {
                        contraTI.setError("Mínimo 6 caracteres");
                    } else {
                        fAuth.getCurrentUser().updatePassword(nuevaContra)
                                .addOnCompleteListener(task -> {
                                    if (task.isSuccessful()) {
                                        Toast.makeText(PerfilActivity.this, "Contraseña actualizada", Toast.LENGTH_SHORT).show();
                                        contraTI.setText("");
                                    } else {
                                        Toast.makeText(PerfilActivity.this, "Cierra e inicia sesión de nuevo para ver los cambios en la contraseña", Toast.LENGTH_LONG).show();
                                    }
                                });
                    }
                }
            }
        });


    }

    public void logout (View view) {
        FirebaseAuth.getInstance().signOut();
        Toast.makeText(PerfilActivity.this, "Sesión cerrada correctamente", Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(PerfilActivity.this, WelcomeActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void cargarDatosUsuario() {
        DocumentReference docRef = fStore.collection("usuarios").document(userID);
        docRef.get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                String nombre = documentSnapshot.getString("nombreUsuario");
                String email = documentSnapshot.getString("email");

                nombreTI.setText(nombre);
                emailTI.setText(email);
            }
        }).addOnFailureListener(e -> {
            Toast.makeText(this, "Error al cargar datos", Toast.LENGTH_SHORT).show();
        });

        StorageReference profileRef = storageReference.child("users/" + userID + "/profile.jpg");

        profileRef.getDownloadUrl().addOnSuccessListener(uri -> {
            Glide.with(this)
                    .load(uri)
                    .placeholder(R.drawable.mascot)
                    .error(R.drawable.mascot)
                    .circleCrop()
                    .diskCacheStrategy(com.bumptech.glide.load.engine.DiskCacheStrategy.NONE)
                    .skipMemoryCache(true)
                    .into(fotoIB);
        }).addOnFailureListener(e -> {
            Log.e("Storage", "El usuario no tiene foto o hubo un error.");
        });
    }
    private void mostrarOpcionesFoto() {
        String[] opciones = {"Hacer foto", "Elegir de galería", "Cancelar"};
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("Actualizar foto de perfil");
        builder.setItems(opciones, (dialog, which) -> {
            if (which == 0) {
                abrirCamara();
            } else if (which == 1) {
                Intent intent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                galleryLauncher.launch(intent);
            } else {
                dialog.dismiss();
            }
        });
        builder.show();
    }

    private void abrirCamara() {
        android.content.ContentValues values = new android.content.ContentValues();
        values.put(android.provider.MediaStore.Images.Media.TITLE, "Nueva Foto");
        cameraUri = getContentResolver().insert(android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
        cameraLauncher.launch(cameraUri);
    }

    private void uploadImageAndFinish(android.net.Uri uri) {
        StorageReference fileRef = storageReference.child("users/" + userID + "/profile.jpg");

        fileRef.putFile(uri).addOnSuccessListener(taskSnapshot -> {
            Toast.makeText(PerfilActivity.this, "Perfil actualizado con éxito", Toast.LENGTH_SHORT).show();
            finish();
        }).addOnFailureListener(e -> {
            Toast.makeText(PerfilActivity.this, "Error al subir foto", Toast.LENGTH_SHORT).show();
        });
    }

}