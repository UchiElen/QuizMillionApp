package com.dam.quizmillionapp.models;

import com.google.firebase.Timestamp;

public class RoomMember {

    private String displayName;
    private Timestamp joinedAt;
    private boolean isHost;
    private boolean isReady;

    public RoomMember() {
    }

    public RoomMember(String displayName, Timestamp joinedAt, boolean isHost, boolean isReady) {
        this.displayName = displayName;
        this.joinedAt = joinedAt;
        this.isHost = isHost;
        this.isReady = isReady;
    }

    public String getDisplayName() {
        return displayName;
    }

    public Timestamp getJoinedAt() {
        return joinedAt;
    }

    public boolean isHost() {
        return isHost;
    }

    public boolean isReady() {
        return isReady;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public void setJoinedAt(Timestamp joinedAt) {
        this.joinedAt = joinedAt;
    }

    public void setHost(boolean host) {
        isHost = host;
    }

    public void setReady(boolean ready) {
        isReady = ready;
    }
}