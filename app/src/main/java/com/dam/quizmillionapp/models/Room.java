package com.dam.quizmillionapp.models;

public class Room {

    private String code;
    private Long createdAt;
    private String hostUid;
    private int maxPlayers;
    private int playerCount;
    private String status;
    private Long startedAt;
    private Long closedAt;

    public Room() {
    }

    public Room(String code, Long createdAt, String hostUid, int maxPlayers, int playerCount,
                String status, Long startedAt, Long closedAt) {
        this.code = code;
        this.createdAt = createdAt;
        this.hostUid = hostUid;
        this.maxPlayers = maxPlayers;
        this.playerCount = playerCount;
        this.status = status;
        this.startedAt = startedAt;
        this.closedAt = closedAt;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public Long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Long createdAt) {
        this.createdAt = createdAt;
    }

    public String getHostUid() {
        return hostUid;
    }

    public void setHostUid(String hostUid) {
        this.hostUid = hostUid;
    }

    public int getMaxPlayers() {
        return maxPlayers;
    }

    public void setMaxPlayers(int maxPlayers) {
        this.maxPlayers = maxPlayers;
    }

    public int getPlayerCount() {
        return playerCount;
    }

    public void setPlayerCount(int playerCount) {
        this.playerCount = playerCount;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Long getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(Long startedAt) {
        this.startedAt = startedAt;
    }

    public Long getClosedAt() {
        return closedAt;
    }

    public void setClosedAt(Long closedAt) {
        this.closedAt = closedAt;
    }
}