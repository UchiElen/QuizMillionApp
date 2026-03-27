package com.dam.quizmillionapp.interfaces;

// Callback para devolver el resultado al intentar unirse a una sala.
public interface JoinRoomCallback {
    void onSuccess(String roomId, boolean alreadyJoined);
    void onError(String errorMessage);
}