
package com.dam.quizmillionapp.models;

public class MemberListItem {

    private String displayName;
    private boolean host;

    public MemberListItem(String displayName, boolean host) {
        this.displayName = displayName;
        this.host = host;
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean isHost() {
        return host;
    }
}