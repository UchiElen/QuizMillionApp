package com.dam.quizmillionapp.models;

public class RoomMember {

    private String uid;
    private String displayName;
    private Long joinedAt;
    private Long lastSeenAt;
    private String memberStatus;
    private boolean isHost;
    private boolean isReady;

    public RoomMember() {

    }

    public RoomMember(String uid, String displayName, Long joinedAt, Long lastSeenAt,
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

    public Long getJoinedAt() {
        return joinedAt;
    }

    public void setJoinedAt(Long joinedAt) {
        this.joinedAt = joinedAt;
    }

    public Long getLastSeenAt() {
        return lastSeenAt;
    }

    public void setLastSeenAt(Long lastSeenAt) {
        this.lastSeenAt = lastSeenAt;
    }

    public String getMemberStatus() {
        return memberStatus;
    }

    public void setMemberStatus(String memberStatus) {
        this.memberStatus = memberStatus;
    }

    public boolean isHost() {
        return isHost;
    }

    public void setHost(boolean host) {
        isHost = host;
    }

    public boolean isReady() {
        return isReady;
    }

    public void setReady(boolean ready) {
        isReady = ready;
    }
}