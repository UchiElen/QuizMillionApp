package com.dam.quizmillionapp.auth;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class UserSession {

    public static String getCurrentUid() {

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (user != null) {
            String uid = user.getUid();

            if (uid != null && !uid.trim().isEmpty()) {
                return uid;
            }
        }

        return null;
    }
}