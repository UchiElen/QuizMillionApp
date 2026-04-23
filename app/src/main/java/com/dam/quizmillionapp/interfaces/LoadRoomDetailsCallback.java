
package com.dam.quizmillionapp.interfaces;

// Se usa para escuchar los cambios de una sala en tiempo real
// y actualizar su estado en la pantalla (nombre, estado, jugadores, etc.)
public interface LoadRoomDetailsCallback {

    void onRoomLoaded(String code, String roomName, boolean isPublic,
                      String status, String hostUid, int loadedMaxPlayers);

    void onRoomNotFound();

    void onError(String errorMessage);
}