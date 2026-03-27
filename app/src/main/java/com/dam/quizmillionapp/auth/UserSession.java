package com.dam.quizmillionapp.auth;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.firebase.BuildConfig;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.UUID;

public class UserSession {

    private static final String PREFS_NAME = "quizmillion_user_session";
    private static final String KEY_UID = "uid";
    private static final String KEY_DISPLAY_NAME = "display_name";

    // Se usó para probar las salas cuando aún no estaba hecho el login.
    // Si no hay usuario, crea uno local solo en debug.
    // En producción no se usa.
    private static final boolean ALLOW_DEV_FALLBACK = BuildConfig.DEBUG;

    public static String getCurrentUid(Context context) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (user != null && user.getUid() != null && !user.getUid().trim().isEmpty()) {
            return user.getUid();
        }

        // Si no hay usuario autenticado, usamos un uid local solo para pruebas.
        if (ALLOW_DEV_FALLBACK) {
            return getOrCreateLocalUid(context);
        }

        return null;
    }

    public static String getCurrentDisplayName(Context context) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (user != null) {
            String displayName = user.getDisplayName();
            if (displayName != null && !displayName.trim().isEmpty()) {
                return displayName;
            }

            /*
            String email = user.getEmail();
            if (email != null && !email.trim().isEmpty()) {
                return email;
            }
            */
        }

        if (ALLOW_DEV_FALLBACK) {
            SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            return prefs.getString(KEY_DISPLAY_NAME, "Jugador");
        }

        return null;
    }

    public static void setLocalDisplayName(Context context, String displayName) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(KEY_DISPLAY_NAME, displayName).apply();
    }

    public static boolean isRealLoginActive() {
        return FirebaseAuth.getInstance().getCurrentUser() != null;
    }

    private static String getOrCreateLocalUid(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String uid = prefs.getString(KEY_UID, null);

        // Si no existe un uid local, lo generamos una vez y lo guardamos.
        if (uid == null || uid.trim().isEmpty()) {
            uid = "dev_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
            prefs.edit().putString(KEY_UID, uid).apply();
        }

        return uid;
    }

    public static void clearLocalSession(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().remove(KEY_UID).remove(KEY_DISPLAY_NAME).apply();
    }
}