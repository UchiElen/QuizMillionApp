package com.dam.quizmillionapp.models;

import com.google.firebase.Timestamp;

public class RoomMember {

    private String uid;
    private String displayName;
    private Timestamp joinedAt;
    private Timestamp lastSeenAt;
    private String memberStatus;
    private Boolean isHost;
    private Boolean isReady;

    public RoomMember() {
    }

    public RoomMember(String uid, String displayName, Timestamp joinedAt, Timestamp lastSeenAt,
                      String memberStatus, Boolean isHost, Boolean isReady) {
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

    public Boolean getIsHost() {
        return isHost;
    }

    public void setIsHost(Boolean host) {
        isHost = host;
    }

    public Boolean getIsReady() {
        return isReady;
    }

    public void setIsReady(Boolean ready) {
        isReady = ready;
    }
}