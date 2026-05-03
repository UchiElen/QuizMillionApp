package com.dam.quizmillionapp.interfaces;

// Se usa al intentar unirse a una sala, indicando si el usuario
// ya estaba dentro o si se ha unido en ese momento.
public interface JoinRoomCallback {
    void onSuccess(String roomId, boolean alreadyJoined);
    void onError(String errorMessage);
}