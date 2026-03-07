package com.dam.quizmillionapp.models;

public class RoomSummary {
    public String roomId;
    private String roomName;
    private String roomCode;
    private String roomStatus;
    private long playersCount;
    private long playersMax;

    public RoomSummary() {
    }

    public RoomSummary(String roomId, String roomName, String roomCode, String roomStatus, long playersCount, long playersMax) {
        this.roomId = roomId;
        this.roomName = roomName;
        this.roomCode = roomCode;
        this.roomStatus = roomStatus;
        this.playersCount = playersCount;
        this.playersMax = playersMax;
    }

    public String getRoomId() {
        return roomId;
    }

    public void setRoomId(String roomId) {
        this.roomId = roomId;
    }

    public String getRoomName() {
        return roomName;
    }

    public void setRoomName(String roomName) {
        this.roomName = roomName;
    }

    public String getRoomCode() {
        return roomCode;
    }

    public void setRoomCode(String roomCode) {
        this.roomCode = roomCode;
    }

    public String getRoomStatus() {
        return roomStatus;
    }

    public void setRoomStatus(String roomStatus) {
        this.roomStatus = roomStatus;
    }

    public long getPlayersCount() {
        return playersCount;
    }

    public void setPlayersCount(long playersCount) {
        this.playersCount = playersCount;
    }

    public long getPlayersMax() {
        return playersMax;
    }

    public void setPlayersMax(long playersMax) {
        this.playersMax = playersMax;
    }
}
