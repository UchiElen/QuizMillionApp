package com.dam.quizmillionapp;

import static android.content.ContentValues.TAG;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
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
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

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
    StorageReference storageReference;
    private androidx.activity.result.ActivityResultLauncher<Intent> galleryLauncher;
    private androidx.activity.result.ActivityResultLauncher<android.net.Uri> cameraLauncher;
    private android.net.Uri cameraUri;
    private Uri imagenSeleccionadaUri;

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
        storageReference= FirebaseStorage.getInstance().getReference();



        if (fAuth.getCurrentUser() != null) {
            String emailLogueado = fAuth.getCurrentUser().getEmail();
            Toast.makeText(RegisActivity.this, "Usuario ya logueado con: " + emailLogueado, Toast.LENGTH_LONG).show();
            startActivity(new Intent(getApplicationContext(), MainActivity.class));
            finish();

        }

        galleryLauncher = registerForActivityResult(
                new androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == android.app.Activity.RESULT_OK && result.getData() != null) {
                        imagenSeleccionadaUri = result.getData().getData();

                        // USAR PICASSO PARA RECORTAR EN CÍRCULO
                        com.squareup.picasso.Picasso.get()
                                .load(imagenSeleccionadaUri)
                                .transform(new CircleTransform()) // Aplicamos el recorte circular
                                .into(fotoIB);
                    }
                }
        );

        cameraLauncher = registerForActivityResult(
                new androidx.activity.result.contract.ActivityResultContracts.TakePicture(),
                success -> {
                    if (success && cameraUri != null) {
                        imagenSeleccionadaUri = cameraUri;
                        com.squareup.picasso.Picasso.get()
                                .load(imagenSeleccionadaUri)
                                .transform(new CircleTransform()) // Aplicamos el recorte circular
                                .into(fotoIB);
                    }
                }
        );

        fotoIB.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String[] opciones = {"Hacer foto", "Elegir de galería", "Cancelar"};

                android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(RegisActivity.this);
                builder.setTitle("Selecciona una foto");
                builder.setItems(opciones, (dialog, which) -> {
                    if (which == 0) {
                        abrirCamara();
                    } else if (which == 1) {
                        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                        intent.setType("image/*");
                        galleryLauncher.launch(intent);
                    } else {
                        dialog.dismiss();
                    }
                });
                builder.show();

            }
        });

        AuthBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String email = emailTI.getText().toString().trim();
                String contrasena = contraTI.getText().toString().trim();
                String nombre = nombreTI.getText().toString().trim();

                if (TextUtils.isEmpty(nombre)) {
                    nombreTI.setError("Se requiere un nombre de usuario.");
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
                            userID = fAuth.getCurrentUser().getUid();
                            DocumentReference documentReference = fStore.collection("usuarios").document(userID);
                            Map<String, Object> user = new HashMap<>();
                            user.put("nombreUsuario", nombre);
                            user.put("email", email);
                            documentReference.set(user);
                            if (imagenSeleccionadaUri != null) {
                                uploadImageToFirebase(imagenSeleccionadaUri);
                            } else {
                                Toast.makeText(RegisActivity.this, "Usuario creado sin foto.", Toast.LENGTH_SHORT).show();
                                finalizarRegistro();
                            }

                            Toast.makeText(RegisActivity.this, "Usuario creado correctamente.", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }

        });

    }
    private void abrirCamara() {
        android.content.ContentValues values = new android.content.ContentValues();
        values.put(MediaStore.Images.Media.TITLE, "Nueva Foto");
        values.put(MediaStore.Images.Media.DESCRIPTION, "Desde la Cámara");
        cameraUri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
        if (cameraUri != null) {
            cameraLauncher.launch(cameraUri);
        }
    }

    private void uploadImageToFirebase(Uri imageUri){
        StorageReference fileRef = storageReference.child("users/" + userID + "/profile.jpg");
        fileRef.putFile(imageUri).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                Log.d(TAG, "Imagen subida al storage para el usuario: " + userID);
                finalizarRegistro();
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Log.e(TAG, "Fallo al subir imagen: " + e.getMessage());
                finalizarRegistro();
            }
        });

    }


    private void finalizarRegistro() {
        progressBar.setVisibility(View.GONE);
        startActivity(new Intent(getApplicationContext(), MainActivity.class));
        finish();
    }

    public class CircleTransform implements com.squareup.picasso.Transformation {
        @Override
        public android.graphics.Bitmap transform(android.graphics.Bitmap source) {
            int size = Math.min(source.getWidth(), source.getHeight());
            int x = (source.getWidth() - size) / 2;
            int y = (source.getHeight() - size) / 2;
            android.graphics.Bitmap squaredBitmap = android.graphics.Bitmap.createBitmap(source, x, y, size, size);
            if (squaredBitmap != source) source.recycle();
            android.graphics.Bitmap bitmap = android.graphics.Bitmap.createBitmap(size, size, source.getConfig());
            android.graphics.Canvas canvas = new android.graphics.Canvas(bitmap);
            android.graphics.Paint paint = new android.graphics.Paint();
            android.graphics.BitmapShader shader = new android.graphics.BitmapShader(squaredBitmap,
                    android.graphics.Shader.TileMode.CLAMP, android.graphics.Shader.TileMode.CLAMP);
            paint.setShader(shader);
            paint.setAntiAlias(true);
            float r = size / 2f;
            canvas.drawCircle(r, r, r, paint);
            squaredBitmap.recycle();
            return bitmap;
        }
        @Override public String key() { return "circle"; }
    }
}
