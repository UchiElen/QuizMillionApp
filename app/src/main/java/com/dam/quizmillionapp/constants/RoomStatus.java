package com.dam.quizmillionapp.constants;

// Estados que definen el ciclo de vida de una sala
// y controlan el acceso de jugadores y el inicio de la partida.
public final class RoomStatus {

    public static final String OPEN = "OPEN";
    public static final String FULL = "FULL";
    public static final String IN_GAME = "IN_GAME";
    public static final String FINISHED = "FINISHED";
    public static final String CANCELLED = "CANCELLED";

    private RoomStatus() {
    }
}