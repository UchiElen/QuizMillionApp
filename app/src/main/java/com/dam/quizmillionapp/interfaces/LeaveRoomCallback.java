package com.dam.quizmillionapp.interfaces;

// Callback para saber si el usuario ha salido correctamente de la sala.
public interface LeaveRoomCallback {
    void onSuccess();
    void onError(String errorMessage);
}