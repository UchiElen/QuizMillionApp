
package com.dam.quizmillionapp.repositories;

import com.google.firebase.firestore.FirebaseFirestore;

public class UserRepository {

    private final FirebaseFirestore db;

    // callback para devolver el nombre del usuario
    public interface OnUserNameLoadedCallback {
        void onSuccess(String userName);
        void onError(String errorMessage);
    }

    public UserRepository() {
        db = FirebaseFirestore.getInstance();
    }

    // busca el nombre del usuario por su uid
    public void getUserNameByUid(String uid, OnUserNameLoadedCallback callback) {

        db.collection("usuarios")
                .document(uid)
                .get()
                .addOnSuccessListener(documentSnapshot -> {

                    if (documentSnapshot.exists()) {
                        String nombreUsuario = documentSnapshot.getString("nombreUsuario");

                        // si no hay nombre, usamos uno por defecto
                        if (nombreUsuario != null && !nombreUsuario.trim().isEmpty()) {
                            callback.onSuccess(nombreUsuario.trim());
                        } else {
                            callback.onSuccess("Jugador");
                        }

                    } else {
                        callback.onSuccess("Jugador");
                    }

                })
                .addOnFailureListener(e ->
                        callback.onError("Error cargando usuario: " + e.getMessage())
                );
    }
}