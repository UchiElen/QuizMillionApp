
package com.dam.quizmillionapp.interfaces;

public interface CreateRoomCallback {
    void onSuccess(String roomId);
    void onError(String errorMessage);
}