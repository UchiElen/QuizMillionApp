package com.dam.quizmillionapp.models;

// Modelo simplificado de un jugador usado para mostrar
// la lista de miembros en la sala (nombre y si es host).
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