package com.dam.quizmillionapp.interfaces;

// Se utiliza al abandonar una sala para controlar el resultado
// después de actualizar miembros, contador y posible cambio de host.
public interface LeaveRoomCallback {
    void onSuccess();
    void onError(String errorMessage);
}