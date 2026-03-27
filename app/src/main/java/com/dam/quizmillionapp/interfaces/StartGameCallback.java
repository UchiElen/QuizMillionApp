package com.dam.quizmillionapp.interfaces;

// Callback para indicar si la partida se ha iniciado correctamente.
public interface StartGameCallback {
    void onSuccess();
    void onError(String errorMessage);
}