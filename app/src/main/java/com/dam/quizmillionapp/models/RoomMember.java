package com.dam.quizmillionapp.models;

import com.google.firebase.Timestamp;

// Modelo que representa a un jugador dentro de una sala,
// incluyendo su presencia, rol y estado básico.
public class RoomMember {

    private String uid;
    private String displayName;
    private Timestamp joinedAt;
    private Timestamp lastSeenAt;
    private String memberStatus;
    private boolean isHost;
    private boolean isReady;

    public RoomMember() {
    }

    public RoomMember(String uid, String displayName, Timestamp joinedAt, Timestamp lastSeenAt,
                      String memberStatus, boolean isHost, boolean isReady) {

        this.uid = uid;
        this.displayName = displayName;
        this.joinedAt = joinedAt;
        this.lastSeenAt = lastSeenAt;
        this.memberStatus = memberStatus;
        this.isHost = isHost;
        this.isReady = isReady;
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public Timestamp getJoinedAt() {
        return joinedAt;
    }

    public void setJoinedAt(Timestamp joinedAt) {
        this.joinedAt = joinedAt;
    }

    public Timestamp getLastSeenAt() {
        return lastSeenAt;
    }

    public void setLastSeenAt(Timestamp lastSeenAt) {
        this.lastSeenAt = lastSeenAt;
    }

    public String getMemberStatus() {
        return memberStatus;
    }

    public void setMemberStatus(String memberStatus) {
        this.memberStatus = memberStatus;
    }

    public boolean getIsHost() {
        return isHost;
    }

    public void setIsHost(boolean host) {
        isHost = host;
    }

    public boolean getIsReady() {
        return isReady;
    }

    public void setIsReady(boolean ready) {
        isReady = ready;
    }
}