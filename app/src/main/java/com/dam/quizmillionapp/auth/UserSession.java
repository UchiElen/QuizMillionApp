package com.dam.quizmillionapp.auth;

import android.content.SharedPreferences;
import android.content.Context;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import java.util.UUID;

public class UserSession {

    /*
    //private static final boolean allowDevFallback = true;
    //private static final String DEV_UID = "dev_" + UUID.randomUUID().toString().substring(0,8);
    //private static final String DEV_NAME = "Dev Player";

    public static String getCurrentUid(){
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null){
            return user.getUid();
        }
        if (allowDevFallback){
            return DEV_UID;
        }
        return null;
    }

        public static String getCurrentDisplayName(){
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if(user != null){
            String email = user.getEmail();
            if(email != null && !email.trim().isEmpty()){
                return email;
            }
            return "Player";
        }

        if (allowDevFallback){
            return DEV_NAME;
        }

        return null;
    }

    public static boolean isRealLoginActive(){
        return FirebaseAuth.getInstance().getCurrentUser() != null;
    }
    */

    private static final String PREFS_NAME = "quizmillion_user_session";
    private static final String KEY_UID = "uid";
    private static final String KEY_DISPLAY_NAME = "display_name";
    public static String getOrCreateUid(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String uid = prefs.getString(KEY_UID, null);

        if (uid == null || uid.trim().isEmpty()) {
            uid = "dev_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
            prefs.edit().putString(KEY_UID, uid).apply();
        }

        return uid;
    }
    public static void setDisplayName(Context context, String displayName) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(KEY_DISPLAY_NAME, displayName).apply();
    }

    public static String getCurrentDisplayName(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getString(KEY_DISPLAY_NAME, "Jugador");
    }


}