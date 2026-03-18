package com.dam.quizmillionapp.models;

public class RoomSummary {
    private String roomId;
    private String name;
    private String code;
    private String status;
    private long playerCount;
    private long maxPlayers;
    private String hostUid;

    public RoomSummary() {
    }

    public RoomSummary(String roomId, String name, String code, String status, long playerCount, long maxPlayers, String hostUid) {

        this.roomId = roomId;
        this.name = name;
        this.code = code;
        this.status = status;
        this.playerCount = playerCount;
        this.maxPlayers = maxPlayers;
        this.hostUid = hostUid;
    }

    public String getRoomId() {
        return roomId;
    }

    public void setRoomId(String roomId) {
        this.roomId = roomId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public long getPlayerCount() {
        return playerCount;
    }

    public void setPlayerCount(long playerCount) {
        this.playerCount = playerCount;
    }

    public long getMaxPlayers() {
        return maxPlayers;
    }

    public void setMaxPlayers(long maxPlayers) {
        this.maxPlayers = maxPlayers;
    }

    public String getHostUid() {
        return hostUid;
    }

    public void setHostUid(String hostUid) {
        this.hostUid = hostUid;
    }
}
