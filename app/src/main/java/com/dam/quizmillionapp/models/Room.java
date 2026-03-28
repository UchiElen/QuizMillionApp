package com.dam.quizmillionapp.models;

import com.google.firebase.Timestamp;

// Modelo que representa una sala de juego almacenada en Firestore,
// incluyendo su estado, jugadores y datos necesarios para gestionar la partida.
public class Room {

    private String code;
    private Timestamp createdAt;
    private String hostUid;
    private Long maxPlayers;
    private Long playerCount;
    private String status;
    private Timestamp startedAt;
    private Timestamp closedAt;
    private String name;

    public Room() {
    }

    public Room(String code, Timestamp createdAt, String hostUid, Long maxPlayers, Long playerCount,
                String status, Timestamp startedAt, Timestamp closedAt, String name) {
        this.code = code;
        this.createdAt = createdAt;
        this.hostUid = hostUid;
        this.maxPlayers = maxPlayers;
        this.playerCount = playerCount;
        this.status = status;
        this.startedAt = startedAt;
        this.closedAt = closedAt;
        this.name = name;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public String getHostUid() {
        return hostUid;
    }

    public void setHostUid(String hostUid) {
        this.hostUid = hostUid;
    }

    public Long getMaxPlayers() {
        return maxPlayers;
    }

    public void setMaxPlayers(Long maxPlayers) {
        this.maxPlayers = maxPlayers;
    }

    public Long getPlayerCount() {
        return playerCount;
    }

    public void setPlayerCount(Long playerCount) {
        this.playerCount = playerCount;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Timestamp getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(Timestamp startedAt) {
        this.startedAt = startedAt;
    }

    public Timestamp getClosedAt() {
        return closedAt;
    }

    public void setClosedAt(Timestamp closedAt) {
        this.closedAt = closedAt;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}