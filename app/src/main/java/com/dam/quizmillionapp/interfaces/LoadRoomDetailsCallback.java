package com.dam.quizmillionapp.interfaces;

public interface LoadRoomDetailsCallback {
    // Este metodo recupera los datos de las sala
    void onRoomLoaded(String code, String roomName, boolean isPublic,
                      String status, String hostUid, int loadedMaxPlayers);
    void onRoomNotFound();
    void onError(String errorMessage);
}