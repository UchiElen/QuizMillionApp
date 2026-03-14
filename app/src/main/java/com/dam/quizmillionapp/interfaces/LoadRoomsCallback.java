
package com.dam.quizmillionapp.interfaces;

import com.dam.quizmillionapp.models.RoomSummary;

import java.util.List;

public interface LoadRoomsCallback {
    void onRoomsLoaded(List<RoomSummary> roomList);
    void onError(String errorMessage);
}