package com.dam.quizmillionapp.constants;

// Estados posibles de una sala durante su ciclo de vida.
public final class RoomStatus {

    public static final String OPEN = "OPEN";
    public static final String FULL = "FULL";
    public static final String IN_GAME = "IN_GAME";
    public static final String FINISHED = "FINISHED";
    public static final String CANCELLED = "CANCELLED";

    private RoomStatus() {
    }
}