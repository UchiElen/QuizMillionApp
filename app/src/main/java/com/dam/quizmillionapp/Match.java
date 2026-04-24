package com.dam.quizmillionapp;

import com.google.firebase.Timestamp;

import com.google.firebase.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Locale;

// esta clase sirve para mapear los datos de la subcoleccion Match_history de Firestore
public class Match {
    private String playerName;
    private int score;
    private int levelReached;
    private String mode;
    private String roomName;
    private Timestamp playedAt;

    public Match() {} // constructor vacio necesario para Firebase, como en otros activities

    // Getters
    public String getPlayerName() { return playerName; }
    public int getScore() { return score; }
    public int getLevelReached() { return levelReached; }

    public String getMode() { return mode; }
    public Timestamp getPlayedAt() { return playedAt; }
    public String getRoomName() {
        return (roomName != null) ? roomName : "---";
    }

    public String getFechaFormateada() {
        if (playedAt == null) return "Sin fecha";
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
        return sdf.format(playedAt.toDate());
    }
    //Setters
    public void setRoomName(String roomName) { this.roomName = roomName; }
    public void setMode(String mode) { this.mode = mode; }
}