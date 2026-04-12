package com.dam.quizmillionapp.repositories;

import com.dam.quizmillionapp.constants.MemberStatus;
import com.dam.quizmillionapp.constants.RoomStatus;
import com.dam.quizmillionapp.interfaces.CreateRoomCallback;
import com.dam.quizmillionapp.interfaces.JoinRoomCallback;
import com.dam.quizmillionapp.interfaces.LeaveRoomCallback;
import com.dam.quizmillionapp.interfaces.LoadCategoriesCallback;
import com.dam.quizmillionapp.interfaces.LoadMembersCallback;
import com.dam.quizmillionapp.interfaces.LoadRoomDetailsCallback;
import com.dam.quizmillionapp.interfaces.LoadRoomsCallback;
import com.dam.quizmillionapp.interfaces.StartGameCallback;
import com.dam.quizmillionapp.models.MemberListItem;
import com.dam.quizmillionapp.models.RoomCreationData;
import com.dam.quizmillionapp.models.RoomSummary;
import com.google.firebase.Timestamp;
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

public class RoomRepository {

    private final FirebaseFirestore db;
    private final CollectionReference roomsRef;

    public RoomRepository() {
        db = FirebaseFirestore.getInstance();
        roomsRef = db.collection("rooms");
    }

    private String getDisplayNameOrDefault(String displayName) {
        if (displayName == null || displayName.trim().isEmpty()) {
            return "Jugador";
        }
        return displayName.trim();
    }

    private int getIntOrDefault(Long value, int defaultValue) {
        return value != null ? value.intValue() : defaultValue;
    }

    private String getRoomStatusFromCount(int playerCount, int maxPlayers) {
        return playerCount >= maxPlayers ? RoomStatus.FULL : RoomStatus.OPEN;
    }

    private String generateNewRoomCode() {
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
        Random random = new Random();
        StringBuilder code = new StringBuilder();

        for (int i = 0; i < 6; i++) {
            code.append(chars.charAt(random.nextInt(chars.length())));
        }

        return code.toString().toUpperCase(Locale.ROOT);
    }

    public void createNewRoom(RoomCreationData config, String uid, String displayName, CreateRoomCallback callback) {

        if (config == null) {
            callback.onError("La configuración de la sala está vacía.");
            return;
        }

        if (uid == null || uid.trim().isEmpty()) {
            callback.onError("Usuario no válido.");
            return;
        }

        String roomName = config.getName();
        if (roomName == null || roomName.trim().isEmpty()) {
            roomName = "Room";
        }

        int maxPlayers = (int) config.getMaxPlayers();
        boolean isPublic = config.isPublic();
        List<String> categories = config.getCategories();

        if (categories == null || categories.isEmpty()) {
            categories = new ArrayList<>();
            categories.add("general");
        }

        String playerName = getDisplayNameOrDefault(displayName);
        String roomCode = generateNewRoomCode();

        DocumentReference roomRef = roomsRef.document();
        String roomId = roomRef.getId();
        DocumentReference memberRef = roomRef.collection("members").document(uid);

        Map<String, Object> roomData = new HashMap<>();
        roomData.put("name", roomName.trim());
        roomData.put("code", roomCode);
        roomData.put("hostUid", uid);
        roomData.put("maxPlayers", maxPlayers);
        roomData.put("playerCount", 1);
        roomData.put("status", RoomStatus.OPEN);
        roomData.put("isPublic", isPublic);
        roomData.put("categories", categories);
        roomData.put("createdAt", FieldValue.serverTimestamp());
        roomData.put("startedAt", null);
        roomData.put("closedAt", null);

        Map<String, Object> memberData = new HashMap<>();
        memberData.put("uid", uid);
        memberData.put("displayName", playerName);
        memberData.put("joinedAt", FieldValue.serverTimestamp());
        memberData.put("lastSeenAt", FieldValue.serverTimestamp());
        memberData.put("memberStatus", MemberStatus.ACTIVE);
        memberData.put("isHost", true);
        memberData.put("isReady", false);
        memberData.put("score", 0);

        WriteBatch batch = db.batch();
        batch.set(roomRef, roomData);
        batch.set(memberRef, memberData);

        batch.commit()
                .addOnSuccessListener(unused -> callback.onSuccess(roomId))
                .addOnFailureListener(e -> callback.onError("Error al crear sala: " + e.getMessage()));
    }

    public void joinRoomUsingCode(String roomCode, String currentUid, String displayName, JoinRoomCallback callback) {

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
                    joinRoomDirectly(roomDoc.getId(), currentUid, displayName, callback);
                })
                .addOnFailureListener(e ->
                        callback.onError("Error al buscar por código: " + e.getMessage())
                );
    }

    public void joinRoomDirectly(String roomId, String currentUid, String displayName, JoinRoomCallback callback) {

        if (roomId == null || roomId.trim().isEmpty()) {
            callback.onError("RoomId vacío.");
            return;
        }

        if (currentUid == null || currentUid.trim().isEmpty()) {
            callback.onError("UID vacío.");
            return;
        }

        String playerName = getDisplayNameOrDefault(displayName);

        DocumentReference roomRef = roomsRef.document(roomId);
        DocumentReference memberRef = roomRef.collection("members").document(currentUid);

        db.runTransaction((Transaction.Function<Boolean>) transaction -> {

                    DocumentSnapshot roomSnapshot = transaction.get(roomRef);

                    if (!roomSnapshot.exists()) {
                        throw new RuntimeException("La sala no existe.");
                    }

                    String status = roomSnapshot.getString("status");
                    int playerCount = getIntOrDefault(roomSnapshot.getLong("playerCount"), 0);
                    int maxPlayers = getIntOrDefault(roomSnapshot.getLong("maxPlayers"), 4);

                    if (!RoomStatus.OPEN.equals(status)) {
                        throw new RuntimeException("La sala no está disponible.");
                    }

                    DocumentSnapshot memberSnapshot = transaction.get(memberRef);

                    // Si el jugador ya estaba dentro, no lo duplicamos.
                    // Solo refrescamos su actividad para marcar que sigue en la sala.
                    if (memberSnapshot.exists()) {
                        Map<String, Object> updates = new HashMap<>();
                        updates.put("lastSeenAt", FieldValue.serverTimestamp());
                        updates.put("memberStatus", MemberStatus.ACTIVE);
                        transaction.update(memberRef, updates);
                        return true;
                    }

                    // La transacción evita que entren dos jugadores a la vez
                    // cuando ya solo queda una plaza libre.
                    if (playerCount >= maxPlayers) {
                        throw new RuntimeException("La sala está llena.");
                    }

                    Map<String, Object> memberData = new HashMap<>();
                    memberData.put("uid", currentUid);
                    memberData.put("displayName", playerName);
                    memberData.put("joinedAt", FieldValue.serverTimestamp());
                    memberData.put("lastSeenAt", FieldValue.serverTimestamp());
                    memberData.put("memberStatus", MemberStatus.ACTIVE);
                    memberData.put("isHost", false);
                    memberData.put("isReady", false);
                    memberData.put("score", 0);

                    int newPlayerCount = playerCount + 1;

                    transaction.set(memberRef, memberData);
                    transaction.update(roomRef, "playerCount", newPlayerCount);
                    transaction.update(roomRef, "status", getRoomStatusFromCount(newPlayerCount, maxPlayers));

                    return false;
                })
                .addOnSuccessListener(alreadyJoined ->
                        callback.onSuccess(roomId, alreadyJoined)
                )
                .addOnFailureListener(e ->
                        callback.onError("Error al unirse: " + e.getMessage())
                );
    }

    public ListenerRegistration observeRoomMembers(String roomId, LoadMembersCallback callback) {

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

            String hostUid = roomSnapshot.getString("hostUid");

            roomRef.collection("members")
                    .orderBy("joinedAt", Query.Direction.ASCENDING)
                    .get()
                    .addOnSuccessListener(snapshot -> {

                        List<MemberListItem> members = new ArrayList<>();

                        for (DocumentSnapshot doc : snapshot.getDocuments()) {
                            String uid = doc.getId();
                            String displayName = doc.getString("displayName");

                            if (displayName == null || displayName.trim().isEmpty()) {
                                displayName = "Jugador";
                            }

                            boolean isHost = hostUid != null && hostUid.equals(uid);
                            members.add(new MemberListItem(displayName, isHost));
                        }

                        callback.onMembersLoaded(members);
                    })
                    .addOnFailureListener(e ->
                            callback.onError("Error cargando miembros: " + e.getMessage())
                    );
        });
    }

    public ListenerRegistration observeRoomInfo(String roomId, LoadRoomDetailsCallback callback) {

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
                    String roomName = snapshot.getString("name");
                    String status = snapshot.getString("status");
                    String hostUid = snapshot.getString("hostUid");

                    Boolean publicValue = snapshot.getBoolean("isPublic");
                    boolean isPublic = publicValue != null && publicValue;

                    int maxPlayers = getIntOrDefault(snapshot.getLong("maxPlayers"), 0);

                    callback.onRoomLoaded(code, roomName, isPublic, status, hostUid, maxPlayers);
                });
    }

    public void tryStartGame(String roomId, String userId, StartGameCallback callback) {

        if (roomId == null || roomId.trim().isEmpty()) {
            callback.onError("RoomId vacío.");
            return;
        }

        if (userId == null || userId.trim().isEmpty()) {
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
                    int playerCount = getIntOrDefault(roomSnapshot.getLong("playerCount"), 0);

                    if (hostUid == null || !hostUid.equals(userId)) {
                        throw new RuntimeException("Solo el host puede iniciar la partida.");
                    }

                    boolean canStart = RoomStatus.OPEN.equals(status) || RoomStatus.FULL.equals(status);

                    if (!canStart) {
                        throw new RuntimeException("La sala no está lista para iniciar.");
                    }

                    // No dejamos empezar una partida solo con una persona.
                    if (playerCount < 2) {
                        throw new RuntimeException("No hay jugadores suficientes.");
                    }

                    Map<String, Object> updates = new HashMap<>();
                    updates.put("status", RoomStatus.IN_GAME);
                    updates.put("startedAt", FieldValue.serverTimestamp());

                    transaction.update(roomRef, updates);
                    return null;
                })
                .addOnSuccessListener(unused -> callback.onSuccess())
                .addOnFailureListener(e -> {
                    String errorMessage = e.getMessage() != null
                            ? e.getMessage()
                            : "Error al iniciar la partida.";

                    callback.onError(errorMessage);
                });
    }

    public ListenerRegistration observeOpenRooms(LoadRoomsCallback callback) {

        return roomsRef
                .whereEqualTo("status", RoomStatus.OPEN)
                .whereEqualTo("isPublic", true)
                .addSnapshotListener((snapshot, error) -> {

                    if (error != null) {
                        callback.onError("Error cargando salas: " + error.getMessage());
                        return;
                    }

                    List<RoomSummary> rooms = new ArrayList<>();

                    if (snapshot != null) {
                        for (DocumentSnapshot doc : snapshot.getDocuments()) {

                            String name = doc.getString("name");
                            String code = doc.getString("code");
                            String status = doc.getString("status");
                            String hostUid = doc.getString("hostUid");

                            int playerCount = getIntOrDefault(doc.getLong("playerCount"), 0);
                            int maxPlayers = getIntOrDefault(doc.getLong("maxPlayers"), 0);

                            String roomName = (name != null && !name.trim().isEmpty())
                                    ? name.trim()
                                    : "Sala sin nombre";

                            String roomCode = code != null ? code : "";
                            String roomStatus = status != null ? status : "unknown";

                            // Si por cualquier desfase una sala aparece llena,
                            // la ocultamos del lobby para no mostrar algo que ya no admite jugadores.
                            if (maxPlayers > 0 && playerCount >= maxPlayers) {
                                continue;
                            }

                            rooms.add(new RoomSummary(
                                    doc.getId(),
                                    roomName,
                                    roomCode,
                                    roomStatus,
                                    playerCount,
                                    maxPlayers,
                                    hostUid
                            ));
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
                    boolean isLeavingHost = hostUid != null && hostUid.equals(uid);

                    int currentPlayerCount = getIntOrDefault(roomSnapshot.getLong("playerCount"), 1);
                    if (currentPlayerCount < 1) {
                        currentPlayerCount = 1;
                    }

                    int remainingPlayers = currentPlayerCount - 1;
                    if (remainingPlayers < 0) {
                        remainingPlayers = 0;
                    }

                    if (isLeavingHost) {
                        leaveRoomAsHost(roomRef, membersRef, leavingMemberRef, uid, remainingPlayers, callback);
                    } else {
                        leaveRoomAsPlayer(roomRef, leavingMemberRef, remainingPlayers, callback);
                    }
                })
                .addOnFailureListener(e ->
                        callback.onError("Error leyendo la sala: " + e.getMessage())
                );
    }

    private void leaveRoomAsPlayer(DocumentReference roomRef,
                                   DocumentReference leavingMemberRef,
                                   int remainingPlayers,
                                   LeaveRoomCallback callback) {

        roomRef.get()
                .addOnSuccessListener(roomSnapshot -> {

                    int maxPlayers = getIntOrDefault(roomSnapshot.getLong("maxPlayers"), 4);

                    WriteBatch batch = db.batch();
                    batch.delete(leavingMemberRef);

                    // Si era el último jugador, ya no tiene sentido mantener la sala.
                    if (remainingPlayers == 0) {
                        batch.delete(roomRef);
                    } else {
                        batch.update(roomRef, "playerCount", remainingPlayers);
                        batch.update(roomRef, "status", getRoomStatusFromCount(remainingPlayers, maxPlayers));
                    }

                    batch.commit()
                            .addOnSuccessListener(unused -> callback.onSuccess())
                            .addOnFailureListener(e ->
                                    callback.onError("Error al salir de la sala: " + e.getMessage())
                            );
                })
                .addOnFailureListener(e ->
                        callback.onError("Error leyendo sala: " + e.getMessage())
                );
    }

    private void leaveRoomAsHost(DocumentReference roomRef,
                                 CollectionReference membersRef,
                                 DocumentReference leavingMemberRef,
                                 String leavingUid,
                                 int remainingPlayers,
                                 LeaveRoomCallback callback) {

        membersRef.orderBy("joinedAt", Query.Direction.ASCENDING)
                .get()
                .addOnSuccessListener(memberSnapshots -> {

                    String nextHostUid = null;

                    // Si sale el host, le damos el control al siguiente jugador
                    // que siga dentro de la sala.
                    for (DocumentSnapshot doc : memberSnapshots.getDocuments()) {
                        String candidateUid = doc.getId();

                        if (!candidateUid.equals(leavingUid)) {
                            nextHostUid = candidateUid;
                            break;
                        }
                    }

                    final String newHostUid = nextHostUid;

                    roomRef.get()
                            .addOnSuccessListener(roomSnapshot -> {

                                int maxPlayers = getIntOrDefault(roomSnapshot.getLong("maxPlayers"), 4);

                                WriteBatch batch = db.batch();
                                batch.delete(leavingMemberRef);

                                if (remainingPlayers == 0) {
                                    batch.delete(roomRef);
                                } else {
                                    if (newHostUid == null || newHostUid.trim().isEmpty()) {
                                        callback.onError("No se pudo asignar nuevo anfitrión.");
                                        return;
                                    }

                                    batch.update(roomRef, "playerCount", remainingPlayers);
                                    batch.update(roomRef, "hostUid", newHostUid);
                                    batch.update(roomRef, "status", getRoomStatusFromCount(remainingPlayers, maxPlayers));

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
                                    callback.onError("Error leyendo sala: " + e.getMessage())
                            );
                })
                .addOnFailureListener(e ->
                        callback.onError("Error buscando nuevo anfitrión: " + e.getMessage())
                );
    }

    public void updateUserActivity(String roomId, String userId) {
        roomsRef.document(roomId)
                .collection("members")
                .document(userId)
                .update("lastSeenAt", FieldValue.serverTimestamp());
    }

    public void fixRoomStateIfNeeded(String roomId) {

        if (roomId == null || roomId.trim().isEmpty()) {
            return;
        }

        DocumentReference roomRef = roomsRef.document(roomId);

        roomRef.get()
                .addOnSuccessListener(roomSnapshot -> {

                    if (roomSnapshot == null || !roomSnapshot.exists()) {
                        return;
                    }

                    int maxPlayers = getIntOrDefault(roomSnapshot.getLong("maxPlayers"), 4);

                    roomRef.collection("members")
                            .get()
                            .addOnSuccessListener(memberSnapshots -> {

                                int realCount = memberSnapshots.size();

                                // Si ya no queda nadie dentro, eliminamos la sala.
                                if (realCount == 0) {
                                    roomRef.delete();
                                    return;
                                }

                                Map<String, Object> updates = new HashMap<>();
                                updates.put("playerCount", realCount);
                                updates.put("status", getRoomStatusFromCount(realCount, maxPlayers));

                                roomRef.update(updates);
                            });
                });
    }

    public void removeInactivePlayers(String roomId) {

        if (roomId == null || roomId.trim().isEmpty()) {
            return;
        }

        DocumentReference roomRef = roomsRef.document(roomId);

        roomRef.collection("members")
                .get()
                .addOnSuccessListener(memberSnapshots -> {

                    long now = System.currentTimeMillis();
                    long timeoutMillis = 60000;

                    WriteBatch batch = db.batch();
                    boolean hasChanges = false;

                    for (DocumentSnapshot doc : memberSnapshots.getDocuments()) {

                        Timestamp lastSeenAt = doc.getTimestamp("lastSeenAt");

                        if (lastSeenAt != null) {
                            long lastSeenMillis = lastSeenAt.toDate().getTime();
                            long diff = now - lastSeenMillis;

                            // Si alguien lleva demasiado tiempo sin actividad,
                            // lo quitamos para que la sala no se quede desfasada.
                            if (diff > timeoutMillis) {
                                batch.delete(doc.getReference());
                                hasChanges = true;
                            }
                        }
                    }

                    // Solo recalculamos la sala si realmente se borró alguien.
                    if (hasChanges) {
                        batch.commit()
                                .addOnSuccessListener(unused -> fixRoomStateIfNeeded(roomId));
                    }
                });
    }

    public void getAvailableCategories(LoadCategoriesCallback callback) {

        db.collection("preguntas")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {

                    List<String> uniqueCategories = new ArrayList<>();

                    for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                        String category = doc.getString("categoria");

                        if (category != null && !category.trim().isEmpty()) {
                            String cleanCategory = category.trim().toLowerCase(Locale.ROOT);

                            if (!uniqueCategories.contains(cleanCategory)) {
                                uniqueCategories.add(cleanCategory);
                            }
                        }
                    }

                    uniqueCategories.sort(String::compareTo);
                    callback.onCategoriesLoaded(uniqueCategories);
                })
                .addOnFailureListener(e ->
                        callback.onError("Error cargando categorías: " + e.getMessage())
                );
    }
}