package com.dam.quizmillionapp;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Shader;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Transformation;

public class PerfilActivity extends BaseActivity {

    TextInputEditText nombreTI, emailTI, contraTI;
    ImageButton fotoIB;
    FirebaseAuth fAuth;
    FirebaseFirestore fStore;
    StorageReference storageReference;
    String userID;


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
        fAuth = FirebaseAuth.getInstance();
        fStore = FirebaseFirestore.getInstance();
        storageReference = FirebaseStorage.getInstance().getReference();


        //String emailLogueado = fAuth.getCurrentUser().getEmail();
        //emailTI.setText(emailLogueado);
        //String userID = fAuth.getCurrentUser().getUid();

        if (fAuth.getCurrentUser() != null) {
            userID = fAuth.getCurrentUser().getUid();
            cargarDatosUsuario();
        }
    }

    /*StorageReference profileRef = storage.getReference().child("users/" + userID + "/profile.jpg");

    profileRef.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
        @Override
        public void onSuccess(Uri uri) {
            // 4. ¡Aquí entra Picasso!
            Picasso.get()
                    .load(uri)
                    .placeholder(R.drawable.usuario_default) // Imagen mientras carga
                    .error(R.drawable.error_imagen)         // Imagen si algo falla
                    .into(perfilIV);
        }
    }).addOnFailureListener(new OnFailureListener() {
        @Override
        public void onFailure(@NonNull Exception e) {
            Log.e("TAG", "No se pudo obtener la URL: " + e.getMessage());
            // Aquí podrías poner una imagen por defecto si el usuario no tiene una
        }
    });*/
    public void logout (View view) {
        FirebaseAuth.getInstance().signOut();
        Toast.makeText(PerfilActivity.this, "Sesión cerrada correctamente", Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(PerfilActivity.this, WelcomeActivity.class);
        startActivity(intent);
        finish();
    }

    private void cargarDatosUsuario() {
        // --- PARTE 1: TRAER EL NOMBRE DE FIRESTORE ---
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

        // --- PARTE 2: TRAER LA FOTO DE STORAGE CON PICASSO ---
        StorageReference profileRef = storageReference.child("users/" + userID + "/profile.jpg");

        profileRef.getDownloadUrl().addOnSuccessListener(uri -> {
            Picasso.get()
                    .load(uri)
                    .placeholder(R.drawable.mascot)
                    .error(R.drawable.mascot)
                    .transform(new CircleTransform())
                    .into(fotoIB);
        }).addOnFailureListener(e -> {
            Log.e("Storage", "El usuario no tiene foto o hubo un error.");
        });
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