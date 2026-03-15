package com.dam.quizmillionapp.auth;

import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

public class FirebaseConnectionChecker {

    private static final String tag = "FirebaseCheck";

    public static void checkFirestoreConnection(){
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("questions")
                .limit(1)
                .get()
                .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                    @Override
                    public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                        Log.d(tag,"Firestore OK. Docs: " + queryDocumentSnapshots.size() );
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.e(tag, "Firestore FAIL: " + e.getMessage() );
                    }
                });

    }














}
