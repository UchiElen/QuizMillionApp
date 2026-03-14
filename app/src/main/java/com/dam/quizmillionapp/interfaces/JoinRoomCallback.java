
package com.dam.quizmillionapp.interfaces;

public interface JoinRoomCallback {
    void onSuccess(String roomId, boolean alreadyJoined);
    void onError(String errorMessage);
}