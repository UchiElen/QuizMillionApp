package com.dam.quizmillionapp.interfaces;

// Se utiliza al iniciar la partida para confirmar que la sala ha pasado
// correctamente a estado IN_GAME tras validar condiciones como host y jugadores.
public interface StartGameCallback {
    void onSuccess();
    void onError(String errorMessage);
}