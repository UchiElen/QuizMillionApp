package com.dam.quizmillionapp.interfaces;

// Se usa para gestionar el resultado de la creación de una sala,
// separando el flujo de éxito y error al trabajar con Firestore.
public interface CreateRoomCallback {
    void onSuccess(String roomId);
    void onError(String errorMessage);
}