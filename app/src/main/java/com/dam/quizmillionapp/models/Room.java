package com.dam.quizmillionapp.models;

import com.google.firebase.Timestamp;

public class Room {

    private String code;
    private Timestamp createdAt;
    private String hostUid;
    private int maxPlayers;
    private int playerCount;
    private String status;

    public Room() {
    }

    public Room(String code, Timestamp createdAt, String hostUid, int maxPlayers, int playerCount, String status) {
        this.code = code;
        this.createdAt = createdAt;
        this.hostUid = hostUid;
        this.maxPlayers = maxPlayers;
        this.playerCount = playerCount;
        this.status = status;
    }

    public String getCode() {
        return code;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public String getHostUid() {
        return hostUid;
    }

    public int getMaxPlayers() {
        return maxPlayers;
    }

    public int getPlayerCount() {
        return playerCount;
    }

    public String getStatus() {
        return status;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public void setHostUid(String hostUid) {
        this.hostUid = hostUid;
    }

    public void setMaxPlayers(int maxPlayers) {
        this.maxPlayers = maxPlayers;
    }

    public void setPlayerCount(int playerCount) {
        this.playerCount = playerCount;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}