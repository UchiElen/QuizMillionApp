
package com.dam.quizmillionapp.interfaces;

// Callback para devolver los datos de una sala en tiempo real.
public interface LoadRoomDetailsCallback {

    void onRoomLoaded(String code, String roomName, boolean isPublic,
                      String status, String hostUid, int loadedMaxPlayers);

    void onRoomNotFound();

    void onError(String errorMessage);
}