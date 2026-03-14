
package com.dam.quizmillionapp.interfaces;

import java.util.List;

public interface LoadMembersCallback {
    void onMembersLoaded(List<String> memberNames);
    void onError(String errorMessage);
}