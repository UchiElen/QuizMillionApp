package com.dam.quizmillionapp.interfaces;

import com.dam.quizmillionapp.models.RoomSummary;

import java.util.List;

// Callback para devolver la lista de salas disponibles.
public interface LoadRoomsCallback {

    // Se llama cuando las salas se han cargado correctamente
    void onRoomsLoaded(List<RoomSummary> roomList);

    void onError(String errorMessage);
}