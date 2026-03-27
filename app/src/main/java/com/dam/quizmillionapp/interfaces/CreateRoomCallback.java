package com.dam.quizmillionapp.interfaces;

// Callback para devolver el resultado al crear una sala.
public interface CreateRoomCallback {
    void onSuccess(String roomId);
    void onError(String errorMessage);
}