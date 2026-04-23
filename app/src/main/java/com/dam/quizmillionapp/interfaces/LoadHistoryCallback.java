package com.dam.quizmillionapp.interfaces;

import com.dam.quizmillionapp.models.MatchHistoryItem;

import java.util.List;

// Esta interfaz se utiliza para recibir el resultado de la carga del historial de partidas.
// Cuando Firebase devuelve los datos, se obtiene una lista de partidas,
// y si ocurre algún error, se muestra el mensaje correspondiente.
public interface LoadHistoryCallback {
    void onHistoryLoaded(List<MatchHistoryItem> historyList);
    void onError(String errorMessage);
}