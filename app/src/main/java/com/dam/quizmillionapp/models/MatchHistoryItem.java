package com.dam.quizmillionapp.models;

import com.google.firebase.Timestamp;

// Esta clase representa el resultado de una partida que ha jugado un usuario.
// Se utiliza para guardar el historial de puntuaciones en Firebase, de forma que cada vez que termina una partida,
// se almacena su puntuación, el modo de juego, el nivel alcanzado y la fecha. Luego se pueden mostrar en pantalla o exportar a PDF.
public class MatchHistoryItem {

    private String matchId;
    private String roomId;
    private String playerUid;
    private String playerName;
    private long score;
    private Timestamp playedAt;
    private String mode;
    private int levelReached;

    public MatchHistoryItem() {
        // Constructor vacío obligatorio para Firestore
    }

    public MatchHistoryItem(String matchId, String roomId, String playerUid, String playerName,
                            long score, Timestamp playedAt, String mode, int levelReached) {
        this.matchId = matchId;
        this.roomId = roomId;
        this.playerUid = playerUid;
        this.playerName = playerName;
        this.score = score;
        this.playedAt = playedAt;
        this.mode = mode;
        this.levelReached = levelReached;
    }

    public String getMatchId() {
        return matchId;
    }

    public void setMatchId(String matchId) {
        this.matchId = matchId;
    }

    public String getRoomId() {
        return roomId;
    }

    public void setRoomId(String roomId) {
        this.roomId = roomId;
    }

    public String getPlayerUid() {
        return playerUid;
    }

    public void setPlayerUid(String playerUid) {
        this.playerUid = playerUid;
    }

    public String getPlayerName() {
        return playerName;
    }

    public void setPlayerName(String playerName) {
        this.playerName = playerName;
    }

    public long getScore() {
        return score;
    }

    public void setScore(long score) {
        this.score = score;
    }

    public Timestamp getPlayedAt() {
        return playedAt;
    }

    public void setPlayedAt(Timestamp playedAt) {
        this.playedAt = playedAt;
    }

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public int getLevelReached() {
        return levelReached;
    }

    public void setLevelReached(int levelReached) {
        this.levelReached = levelReached;
    }
}