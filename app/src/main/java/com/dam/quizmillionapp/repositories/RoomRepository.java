package com.dam.quizmillionapp.repositories;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.dam.quizmillionapp.interfaces.CreateRoomCallback;
import com.dam.quizmillionapp.interfaces.JoinRoomCallback;
import com.dam.quizmillionapp.interfaces.LeaveRoomCallback;
import com.dam.quizmillionapp.interfaces.LoadMembersCallback;
import com.dam.quizmillionapp.interfaces.LoadRoomDetailsCallback;
import com.dam.quizmillionapp.interfaces.LoadRoomsCallback;
import com.dam.quizmillionapp.interfaces.StartGameCallback;
import com.dam.quizmillionapp.models.Room;
import com.dam.quizmillionapp.models.RoomSummary;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.Transaction;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

public class RoomRepository {

    private final FirebaseFirestore db;
    private final CollectionReference roomsRef;

    public RoomRepository() {
        db = FirebaseFirestore.getInstance();
        roomsRef = db.collection("rooms");
    }

    public void createRoom(String roomName, String uid, String displayName, CreateRoomCallback callback) {
        if (uid == null || uid.trim().isEmpty()) {
            callback.onError("UID vacío.");
            return;
        }

        String safeDisplayName = normalizeDisplayName(displayName);

        DocumentReference roomRef = roomsRef.document();
        String roomId = roomRef.getId();
        String roomCode = generateRoomCode();

        Map<String, Object> roomData = new HashMap<>();
        roomData.put("name", roomName != null ? roomName : "Room");
        roomData.put("code", roomCode);
        roomData.put("hostUid", uid);
        roomData.put("maxPlayers", 4L);
        roomData.put("playerCount", 1L);
        roomData.put("status", "waiting");
        roomData.put("createdAt", FieldValue.serverTimestamp());
        roomData.put("startedAt", null);
        roomData.put("closedAt", null);

        Map<String, Object> memberData = new HashMap<>();
        memberData.put("uid", uid);
        memberData.put("displayName", safeDisplayName);
        memberData.put("joinedAt", FieldValue.serverTimestamp());
        memberData.put("lastSeenAt", FieldValue.serverTimestamp());
        memberData.put("memberStatus", "joined");
        memberData.put("isHost", true);
        memberData.put("isReady", false);
        memberData.put("score", 0L);

        DocumentReference memberDoc = roomRef.collection("members").document(uid);

        WriteBatch batch = db.batch();
        batch.set(roomRef, roomData);
        batch.set(memberDoc, memberData);

        batch.commit()
                .addOnSuccessListener(unused -> callback.onSuccess(roomId))
                .addOnFailureListener(e -> callback.onError("Error al crear sala: " + e.getMessage()));
    }

    public void joinRoomByCode(String roomCode, String currentUid, String displayName, JoinRoomCallback callback) {
        if (roomCode == null || roomCode.trim().isEmpty()) {
            callback.onError("Código vacío.");
            return;
        }

        roomsRef.whereEqualTo("code", roomCode.trim().toUpperCase(Locale.ROOT))
                .limit(1)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (queryDocumentSnapshots == null || queryDocumentSnapshots.isEmpty()) {
                        callback.onError("No existe una sala con ese código.");
                        return;
                    }

                    DocumentSnapshot roomDoc = queryDocumentSnapshots.getDocuments().get(0);
                    joinRoomByRoomId(roomDoc.getId(), currentUid, displayName, callback);
                })
                .addOnFailureListener(e -> callback.onError("Error al buscar por código: " + e.getMessage()));
    }

    public void joinRoomByRoomId(String roomId, String currentUid, String displayName, JoinRoomCallback callback) {
        if (roomId == null || roomId.trim().isEmpty()) {
            callback.onError("RoomId vacío.");
            return;
        }

        if (currentUid == null || currentUid.trim().isEmpty()) {
            callback.onError("UID vacío.");
            return;
        }

        final String safeDisplayName = normalizeDisplayName(displayName);
        final DocumentReference roomRef = roomsRef.document(roomId);
        final DocumentReference memberRef = roomRef.collection("members").document(currentUid);

        db.runTransaction((Transaction.Function<Boolean>) transaction -> {
                    DocumentSnapshot roomSnapshot = transaction.get(roomRef);

                    if (!roomSnapshot.exists()) {
                        throw new RuntimeException("La sala no existe.");
                    }

                    String status = roomSnapshot.getString("status");
                    Long playerCount = roomSnapshot.getLong("playerCount");
                    Long maxPlayers = roomSnapshot.getLong("maxPlayers");

                    if (playerCount == null) {
                        playerCount = 0L;
                    }

                    if (maxPlayers == null) {
                        maxPlayers = 4L;
                    }

                    if (status == null || !"waiting".equals(status)) {
                        throw new RuntimeException("La sala no está disponible.");
                    }

                    DocumentSnapshot memberSnapshot = transaction.get(memberRef);

                    if (memberSnapshot.exists()) {
                        transaction.update(memberRef, "lastSeenAt", FieldValue.serverTimestamp());
                        return true;
                    }

                    if (playerCount >= maxPlayers) {
                        throw new RuntimeException("La sala está llena.");
                    }

                    Map<String, Object> memberData = new HashMap<>();
                    memberData.put("uid", currentUid);
                    memberData.put("displayName", safeDisplayName);
                    memberData.put("joinedAt", FieldValue.serverTimestamp());
                    memberData.put("lastSeenAt", FieldValue.serverTimestamp());
                    memberData.put("memberStatus", "joined");
                    memberData.put("isHost", false);
                    memberData.put("isReady", false);
                    memberData.put("score", 0L);

                    transaction.set(memberRef, memberData);
                    transaction.update(roomRef, "playerCount", playerCount + 1L);

                    return false;
                }).addOnSuccessListener(alreadyJoined -> callback.onSuccess(roomId, alreadyJoined))
                .addOnFailureListener(e -> callback.onError("Error al unirse: " + e.getMessage()));
    }

    public ListenerRegistration listenRoomMembers(String roomId, LoadMembersCallback callback) {
        return roomsRef.document(roomId)
                .collection("members")
                .orderBy("joinedAt", Query.Direction.ASCENDING)
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null) {
                        callback.onError("Error cargando miembros: " + error.getMessage());
                        return;
                    }

                    List<String> names = new ArrayList<>();

                    if (snapshot != null) {
                        for (DocumentSnapshot doc : snapshot.getDocuments()) {
                            String name = doc.getString("displayName");
                            Boolean isHost = doc.getBoolean("isHost");

                            if (name == null || name.trim().isEmpty()) {
                                name = doc.getId();
                            }

                            if (Boolean.TRUE.equals(isHost)) {
                                name = name + " (Anfitrión)";
                            }

                            names.add(name);
                        }
                    }

                    callback.onMembersLoaded(names);
                });
    }

    public ListenerRegistration listenRoomDetails(String roomId, LoadRoomDetailsCallback callback) {
        return roomsRef.document(roomId)
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null) {
                        callback.onError("Error escuchando sala: " + error.getMessage());
                        return;
                    }

                    if (snapshot == null || !snapshot.exists()) {
                        callback.onRoomNotFound();
                        return;
                    }

                    String code = snapshot.getString("code");
                    String status = snapshot.getString("status");
                    String hostUid = snapshot.getString("hostUid");

                    callback.onRoomLoaded(code, status, hostUid);
                });
    }

    public void startGameIfHost(String roomId, String uid, StartGameCallback callback) {
        if (roomId == null || roomId.trim().isEmpty()) {
            callback.onError("RoomId vacío.");
            return;
        }

        if (uid == null || uid.trim().isEmpty()) {
            callback.onError("UID vacío.");
            return;
        }

        DocumentReference roomRef = roomsRef.document(roomId);

        db.runTransaction((Transaction.Function<Void>) transaction -> {
                    DocumentSnapshot roomSnapshot = transaction.get(roomRef);

                    if (!roomSnapshot.exists()) {
                        throw new RuntimeException("La sala no existe.");
                    }

                    String hostUid = roomSnapshot.getString("hostUid");
                    String status = roomSnapshot.getString("status");

                    if (hostUid == null || !hostUid.equals(uid)) {
                        throw new RuntimeException("Solo el host puede iniciar.");
                    }

                    if (!"waiting".equals(status)) {
                        throw new RuntimeException("La sala no está en estado waiting.");
                    }

                    transaction.update(roomRef, "status", "in_progress");
                    transaction.update(roomRef, "startedAt", FieldValue.serverTimestamp());
                    return null;
                }).addOnSuccessListener(unused -> callback.onSuccess())
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    public void leaveRoom(String roomId, String uid, LeaveRoomCallback callback) {
        if (roomId == null || roomId.trim().isEmpty()) {
            callback.onError("RoomId vacío.");
            return;
        }

        if (uid == null || uid.trim().isEmpty()) {
            callback.onError("UID vacío.");
            return;
        }

        DocumentReference roomRef = roomsRef.document(roomId);
        DocumentReference memberRef = roomRef.collection("members").document(uid);

        db.runTransaction((Transaction.Function<Void>) transaction -> {
                    DocumentSnapshot roomSnapshot = transaction.get(roomRef);

                    if (!roomSnapshot.exists()) {
                        return null;
                    }

                    DocumentSnapshot memberSnapshot = transaction.get(memberRef);

                    if (!memberSnapshot.exists()) {
                        return null;
                    }

                    Long playerCount = roomSnapshot.getLong("playerCount");
                    if (playerCount == null) {
                        playerCount = 1L;
                    }

                    Boolean wasHost = memberSnapshot.getBoolean("isHost");

                    transaction.delete(memberRef);

                    if (playerCount <= 1L) {
                        transaction.delete(roomRef);
                        return null;
                    }

                    transaction.update(roomRef, "playerCount", playerCount - 1L);

                    if (Boolean.TRUE.equals(wasHost)) {
                        // Transferencia de host todavía no resuelta aquí.
                        // Primero arregla members y luego vienes a por esto.
                    }

                    return null;
                }).addOnSuccessListener(unused -> callback.onSuccess())
                .addOnFailureListener(e -> callback.onError("Error al salir de la sala: " + e.getMessage()));
    }

    public ListenerRegistration listenAvailableRooms(LoadRoomsCallback callback) {
        return roomsRef.whereEqualTo("status", "waiting")
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null) {
                        callback.onError("Error cargando salas: " + error.getMessage());
                        return;
                    }

                    List<RoomSummary> rooms = new ArrayList<>();

                    if (snapshot != null) {
                        for (DocumentSnapshot doc : snapshot.getDocuments()) {
                            String code = doc.getString("code");
                            String status = doc.getString("status");
                            String hostUid = doc.getString("hostUid");
                            Long playerCount = doc.getLong("playerCount");
                            Long maxPlayers = doc.getLong("maxPlayers");

                            RoomSummary summary = new RoomSummary(
                                    doc.getId(),
                                    code != null ? code : "",
                                    status != null ? status : "unknown",
                                    playerCount != null ? playerCount : 0L,
                                    maxPlayers != null ? maxPlayers : 0L,
                                    hostUid
                            );

                            rooms.add(summary);
                        }
                    }

                    callback.onRoomsLoaded(rooms);
                });
    }

    private String normalizeDisplayName(String displayName) {
        if (displayName == null || displayName.trim().isEmpty()) {
            return "Jugador";
        }
        return displayName.trim();
    }

    private String generateRoomCode() {
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
        Random random = new Random();
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < 6; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }

        return sb.toString().toUpperCase(Locale.ROOT);
    }
}