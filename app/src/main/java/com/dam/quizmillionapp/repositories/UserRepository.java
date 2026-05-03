package com.dam.quizmillionapp.repositories;

import com.google.firebase.firestore.FirebaseFirestore;

public class UserRepository {

    private final FirebaseFirestore db;

    // Este callback devuelve el nombre del usuario o un error si la consulta falla.
    public interface OnUserNameLoadedCallback {
        void onSuccess(String userName);
        void onError(String errorMessage);
    }

    public UserRepository() {
        db = FirebaseFirestore.getInstance();
    }

    public void getUserNameByUid(String uid, OnUserNameLoadedCallback callback) {

        if (uid == null || uid.trim().isEmpty()) {
            callback.onError("UID no válido");
            return;
        }

        db.collection("usuarios")
                .document(uid)
                .get()
                .addOnSuccessListener(documentSnapshot -> {

                    String userName = "Jugador";

                    // Si el usuario no tiene nombre guardado, usamos uno por defecto
                    // para no dejar valores vacíos en la interfaz.
                    if (documentSnapshot.exists()) {
                        String storedName = documentSnapshot.getString("nombreUsuario");

                        if (storedName != null && !storedName.trim().isEmpty()) {
                            userName = storedName.trim();
                        }
                    }

                    callback.onSuccess(userName);
                })
                .addOnFailureListener(e ->
                        callback.onError("Error cargando usuario: " + e.getMessage())
                );
    }
}