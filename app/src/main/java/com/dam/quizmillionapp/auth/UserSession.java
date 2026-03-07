package com.dam.quizmillionapp.auth;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class UserSession {

    private static final boolean allowDevFallback = true; // Poner false para el login real

    // Cuál es el UID del usuario actual?
    public static String getCurrentUid(){

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if ( user !=null ){
            return user.getUid();
        }

        if ( allowDevFallback ){
            return "dev_user_1";
        }

        return null;
    }

    // Cuál es el nombre del usuario?
    public static String getCurrentDisplayName(){

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if( user != null ){
            String email = user.getEmail();

            if( email != null && !email.trim().isEmpty() ){
                return email;
            }
            return "Player"; // Si no hay email entonces devuelve un nombre genérico
        }

        if (allowDevFallback){
            return "Dev Player 1"; // Nombre dummy
        }
        return null;
    }

    // El usuario es real o es un test?
    public static boolean isRealLoginActive(){
        return FirebaseAuth.getInstance().getCurrentUser() != null;
    }
}
