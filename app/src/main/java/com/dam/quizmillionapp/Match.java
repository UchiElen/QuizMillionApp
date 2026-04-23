package com.dam.quizmillionapp;

import com.google.firebase.Timestamp;

import com.google.firebase.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Locale;

public class Match {
    private String playerName;
    private int score;
    private int levelReached;
    private String mode;
    private Timestamp playedAt;

    public Match() {} // Necesario para Firebase

    // Getters
    public String getPlayerName() { return playerName; }
    public int getScore() { return score; }
    public int getLevelReached() { return levelReached; }
    public String getMode() { return mode; }
    public Timestamp getPlayedAt() { return playedAt; }

    public String getFechaFormateada() {
        if (playedAt == null) return "Sin fecha";
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
        return sdf.format(playedAt.toDate());
    }
}