


package com.dam.quizmillionapp.repositories;

import com.dam.quizmillionapp.interfaces.CreateRoomCallback;
import com.dam.quizmillionapp.interfaces.JoinRoomCallback;
import com.dam.quizmillionapp.interfaces.LeaveRoomCallback;
import com.dam.quizmillionapp.interfaces.LoadMembersCallback;
import com.dam.quizmillionapp.interfaces.LoadRoomDetailsCallback;
import com.dam.quizmillionapp.interfaces.LoadRoomsCallback;
import com.dam.quizmillionapp.interfaces.StartGameCallback;

import com.dam.quizmillionapp.models.MemberListItem;
import com.dam.quizmillionapp.models.RoomSummary;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.Transaction;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

import com.dam.quizmillionapp.constants.MemberStatus;
import com.dam.quizmillionapp.constants.RoomStatus;

public class RoomRepository {

    private final FirebaseFirestore db;
    private final CollectionReference roomsRef;

    public RoomRepository() {
        db = FirebaseFirestore.getInstance();
        roomsRef = db.collection("rooms");
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
        roomData.put("name", roomName != null && !roomName.trim().isEmpty() ? roomName.trim() : "Room");
        roomData.put("code", roomCode);
        roomData.put("hostUid", uid);
        roomData.put("maxPlayers", 4L);
        roomData.put("playerCount", 1L);
        roomData.put("status", RoomStatus.OPEN);
        roomData.put("createdAt", FieldValue.serverTimestamp());
        roomData.put("startedAt", null);
        roomData.put("closedAt", null);

        Map<String, Object> memberData = new HashMap<>();
        memberData.put("uid", uid);
        memberData.put("displayName", safeDisplayName);
        memberData.put("joinedAt", FieldValue.serverTimestamp());
        memberData.put("lastSeenAt", FieldValue.serverTimestamp());
        memberData.put("memberStatus", MemberStatus.ACTIVE);
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

        if (currentUid == null || currentUid.trim().isEmpty()) {
            callback.onError("UID vacío.");
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

                    if (!RoomStatus.OPEN.equals(status)) {
                        throw new RuntimeException("La sala no está disponible.");
                    }

                    DocumentSnapshot memberSnapshot = transaction.get(memberRef);

                    // Si ya existe como miembro, solo refrescamos presencia
                    if (memberSnapshot.exists()) {
                        Map<String, Object> updates = new HashMap<>();
                        updates.put("lastSeenAt", FieldValue.serverTimestamp());
                        updates.put("memberStatus", MemberStatus.ACTIVE);
                        transaction.update(memberRef, updates);
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
                    memberData.put("memberStatus", MemberStatus.ACTIVE);
                    memberData.put("isHost", false);
                    memberData.put("isReady", false);
                    memberData.put("score", 0L);

                    transaction.set(memberRef, memberData);
                    transaction.update(roomRef, "playerCount", playerCount + 1L);

                    return false;
                })
                .addOnSuccessListener(alreadyJoined -> callback.onSuccess(roomId, alreadyJoined))
                .addOnFailureListener(e -> callback.onError("Error al unirse: " + e.getMessage()));
    }

    public ListenerRegistration listenRoomMembers(String roomId, LoadMembersCallback callback) {
        DocumentReference roomRef = roomsRef.document(roomId);

        return roomRef.addSnapshotListener((roomSnapshot, roomError) -> {
            if (roomError != null) {
                callback.onError("Error leyendo host de sala: " + roomError.getMessage());
                return;
            }

            if (roomSnapshot == null || !roomSnapshot.exists()) {
                callback.onError("La sala ya no existe.");
                return;
            }

            String currentHostUid = roomSnapshot.getString("hostUid");

            roomRef.collection("members")
                    .orderBy("joinedAt", Query.Direction.ASCENDING)
                    .get()
                    .addOnSuccessListener(snapshot -> {
                        List<MemberListItem> members = new ArrayList<>();

                        for (DocumentSnapshot doc : snapshot.getDocuments()) {
                            String uid = doc.getId();
                            String name = doc.getString("displayName");

                            if (name == null || name.trim().isEmpty()) {
                                name = "Jugador";
                            }

                            boolean isHost = currentHostUid != null && currentHostUid.equals(uid);

                            members.add(new MemberListItem(name, isHost));
                        }

                        callback.onMembersLoaded(members);
                    })
                    .addOnFailureListener(e ->
                            callback.onError("Error cargando miembros: " + e.getMessage())
                    );
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
                    Long playerCount = roomSnapshot.getLong("playerCount");

                    if (hostUid == null || !hostUid.equals(uid)) {
                        throw new RuntimeException("Solo el host puede iniciar.");
                    }

                    if (!RoomStatus.OPEN.equals(status)) {
                        throw new RuntimeException("La sala no está lista para iniciar.");
                    }

                    if (playerCount == null || playerCount < 1L) {
                        throw new RuntimeException("No hay jugadores suficientes.");
                    }

                    Map<String, Object> roomUpdates = new HashMap<>();
                    roomUpdates.put("status", RoomStatus.IN_GAME);
                    roomUpdates.put("startedAt", FieldValue.serverTimestamp());

                    transaction.update(roomRef, roomUpdates);
                    return null;
                })
                .addOnSuccessListener(unused -> callback.onSuccess())
                .addOnFailureListener(e -> callback.onError(e.getMessage() != null ? e.getMessage() : "Error al iniciar la partida."));
    }



    public ListenerRegistration listenAvailableRooms(LoadRoomsCallback callback) {
        return roomsRef.whereEqualTo("status", RoomStatus.OPEN)
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

                            long safePlayerCount = playerCount != null ? playerCount : 0L;
                            long safeMaxPlayers = maxPlayers != null ? maxPlayers : 0L;


                            if (safeMaxPlayers > 0 && safePlayerCount >= safeMaxPlayers) {
                                continue;
                            }

                            RoomSummary summary = new RoomSummary(
                                    doc.getId(),
                                    code != null ? code : "",
                                    status != null ? status : "unknown",
                                    safePlayerCount,
                                    safeMaxPlayers,
                                    hostUid
                            );

                            rooms.add(summary);
                        }
                    }

                    callback.onRoomsLoaded(rooms);
                });
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
        DocumentReference leavingMemberRef = roomRef.collection("members").document(uid);
        CollectionReference membersRef = roomRef.collection("members");

        roomRef.get()
                .addOnSuccessListener(roomSnapshot -> {

                    if (roomSnapshot == null || !roomSnapshot.exists()) {
                        callback.onSuccess();
                        return;
                    }

                    String hostUid = roomSnapshot.getString("hostUid");
                    boolean leavingUserWasHost = hostUid != null && hostUid.equals(uid);

                    Long countFromDb = roomSnapshot.getLong("playerCount");
                    long currentPlayerCount = (countFromDb == null || countFromDb < 1L) ? 1L : countFromDb;
                    long remainingPlayers = Math.max(0L, currentPlayerCount - 1L);

                    if (!leavingUserWasHost) {
                        leaveAsNormalPlayer(roomRef, leavingMemberRef, remainingPlayers, callback);
                    } else {
                        leaveAsHost(roomRef, membersRef, leavingMemberRef, uid, remainingPlayers, callback);
                    }
                })
                .addOnFailureListener(e ->
                        callback.onError("Error leyendo la sala: " + e.getMessage())
                );
    }

    private void leaveAsNormalPlayer(DocumentReference roomRef, DocumentReference leavingMemberRef, long remainingPlayers, LeaveRoomCallback callback) {

        WriteBatch batch = db.batch();

        batch.delete(leavingMemberRef);

        if (remainingPlayers == 0L) {
            batch.delete(roomRef);
        } else {
            batch.update(roomRef, "playerCount", remainingPlayers);
        }

        batch.commit()
                .addOnSuccessListener(unused -> callback.onSuccess())
                .addOnFailureListener(e ->
                        callback.onError("Error al salir de la sala: " + e.getMessage())
                );
    }

    private void leaveAsHost(
            DocumentReference roomRef,
            CollectionReference membersRef,
            DocumentReference leavingMemberRef,
            String leavingUid,
            long remainingPlayers,
            LeaveRoomCallback callback) {

        membersRef.orderBy("joinedAt", Query.Direction.ASCENDING)
                .get()
                .addOnSuccessListener(memberSnapshots -> {

                    String newHostUid = null;

                    for (DocumentSnapshot doc : memberSnapshots.getDocuments()) {
                        String candidateUid = doc.getId();

                        if (!candidateUid.equals(leavingUid)) {
                            newHostUid = candidateUid;
                            break;
                        }
                    }

                    WriteBatch batch = db.batch();
                    batch.delete(leavingMemberRef);

                    // Si no quedan jugadores entonces se elimina la sala
                    if (remainingPlayers == 0L) {
                        batch.delete(roomRef);
                    } else {
                        if (newHostUid == null || newHostUid.trim().isEmpty()) {
                            callback.onError("No se pudo asignar nuevo anfitrión.");
                            return;
                        }

                        batch.update(roomRef, "playerCount", remainingPlayers);
                        batch.update(roomRef, "hostUid", newHostUid);

                        DocumentReference newHostRef = membersRef.document(newHostUid);
                        batch.update(newHostRef, "isHost", true);
                    }

                    batch.commit()
                            .addOnSuccessListener(unused -> callback.onSuccess())
                            .addOnFailureListener(e ->
                                    callback.onError("Error al salir de la sala: " + e.getMessage())
                            );
                })
                .addOnFailureListener(e ->
                        callback.onError("Error buscando nuevo anfitrión: " + e.getMessage())
                );
    }



}