package com.dam.quizmillionapp.repositories;

import com.dam.quizmillionapp.interfaces.LoadHistoryCallback;
import com.dam.quizmillionapp.models.MatchHistoryItem;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

// Este repository se encarga de gestionar el historial de resultados del usuario.
// Permite guardar la puntuación final en Firebase y recuperar todas las partidas
// para mostrarlas en la app o exportarlas a PDF.
public class HistoryRepository {

    private final FirebaseFirestore db;

    public HistoryRepository() {
        db = FirebaseFirestore.getInstance();
    }

    // Guarda el resultado de una partida en la subcolección "match_history" del usuario.
    // Se usa el matchId como ID del documento para evitar duplicados.
    public void saveMatchHistory(String userUid, MatchHistoryItem item) {
        db.collection("usuarios")
                .document(userUid)
                .collection("match_history")
                .document(item.getMatchId())
                .set(item);
    }

    // Recupera todas las partidas del usuario ordenadas por fecha.
    // Los datos se convierten en objetos MatchHistoryItem y se devuelven mediante callback.
    public void loadUserHistory(String userUid, LoadHistoryCallback callback) {
        db.collection("usuarios")
                .document(userUid)
                .collection("match_history")
                .orderBy("playedAt")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<MatchHistoryItem> historyList = new ArrayList<>();

                // Convertimos cada documento en un objeto del modelo
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        MatchHistoryItem item = doc.toObject(MatchHistoryItem.class);
                        historyList.add(item);
                    }

                    callback.onHistoryLoaded(historyList);
                })
                .addOnFailureListener(e ->
                        callback.onError("Error al cargar historial: " + e.getMessage())
                );
    }
}