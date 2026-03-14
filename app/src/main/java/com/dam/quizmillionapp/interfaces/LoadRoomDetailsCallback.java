
package com.dam.quizmillionapp.interfaces;

public interface LoadRoomDetailsCallback {
    void onRoomLoaded(String code, String status, String hostUid);
    void onRoomNotFound();
    void onError(String errorMessage);
}