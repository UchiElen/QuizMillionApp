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

    public void createRoom(RoomCreationData config, String uid, String displayName, CreateRoomCallback callback) {

        if (config == null) {
            callback.onError("La configuración de la sala está vacía.");
            return;
        }

        if (uid == null || uid.isEmpty()) {
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

        String safeDisplayName = normalizeDisplayName(displayName);
        String roomCode = generateRoomCode();

        DocumentReference roomRef = roomsRef.document();
        String roomId = roomRef.getId();
        DocumentReference memberDoc = roomRef.collection("members").document(uid);

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
        memberData.put("displayName", safeDisplayName);
        memberData.put("joinedAt", FieldValue.serverTimestamp());
        memberData.put("lastSeenAt", FieldValue.serverTimestamp());
        memberData.put("memberStatus", MemberStatus.ACTIVE);
        memberData.put("isHost", true);
        memberData.put("isReady", false);
        memberData.put("score", 0);

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
                .addOnFailureListener(e ->
                        callback.onError("Error al buscar por código: " + e.getMessage())
                );
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

        String safeDisplayName = normalizeDisplayName(displayName);

        DocumentReference roomRef = roomsRef.document(roomId);
        DocumentReference memberRef = roomRef.collection("members").document(currentUid);

        // La transacción evita que entren dos jugadores a la vez cuando solo queda una plaza libre.
        db.runTransaction((Transaction.Function<Boolean>) transaction -> {

                    DocumentSnapshot roomSnapshot = transaction.get(roomRef);

                    if (!roomSnapshot.exists()) {
                        throw new RuntimeException("La sala no existe.");
                    }

                    String status = roomSnapshot.getString("status");
                    Long playerCountValue = roomSnapshot.getLong("playerCount");
                    Long maxPlayersValue = roomSnapshot.getLong("maxPlayers");

                    int playerCount;
                    if (playerCountValue != null) {
                        playerCount = playerCountValue.intValue();
                    } else {
                        playerCount = 0;
                    }

                    int maxPlayers;
                    if (maxPlayersValue != null) {
                        maxPlayers = maxPlayersValue.intValue();
                    } else {
                        maxPlayers = 4;
                    }

                    if (!RoomStatus.OPEN.equals(status)) {
                        throw new RuntimeException("La sala no está disponible.");
                    }

                    DocumentSnapshot memberSnapshot = transaction.get(memberRef);

                    // Si ya estaba dentro, solo actualizamos su presencia.
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
                    memberData.put("score", 0);

                    transaction.set(memberRef, memberData);

                    int newPlayerCount = playerCount + 1;
                    transaction.update(roomRef, "playerCount", newPlayerCount);

                    // Ajustamos el estado de la sala según el número de jugadores.
                    if (newPlayerCount >= maxPlayers) {
                        transaction.update(roomRef, "status", RoomStatus.FULL);
                    } else {
                        transaction.update(roomRef, "status", RoomStatus.OPEN);
                    }

                    return false;
                })
                .addOnSuccessListener(alreadyJoined ->
                        callback.onSuccess(roomId, alreadyJoined)
                )
                .addOnFailureListener(e ->
                        callback.onError("Error al unirse: " + e.getMessage())
                );
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

                            boolean isHost = false;
                            if (currentHostUid != null && currentHostUid.equals(uid)) {
                                isHost = true;
                            }

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
                    String roomName = snapshot.getString("name");
                    String status = snapshot.getString("status");
                    String hostUid = snapshot.getString("hostUid");

                    Boolean isPublicValue = snapshot.getBoolean("isPublic");
                    boolean isPublic = false;

                    if (isPublicValue != null) {
                        isPublic = isPublicValue;
                    }

                    Long maxPlayersValue = snapshot.getLong("maxPlayers");
                    int maxPlayers = 0;

                    if (maxPlayersValue != null) {
                        maxPlayers = maxPlayersValue.intValue();
                    }

                    callback.onRoomLoaded(code, roomName, isPublic, status, hostUid, maxPlayers);
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

        // Solo el host puede iniciar la partida, y la sala debe estar OPEN o FULL con al menos 2 jugadores.
        db.runTransaction((Transaction.Function<Void>) transaction -> {

                    DocumentSnapshot roomSnapshot = transaction.get(roomRef);

                    if (!roomSnapshot.exists()) {
                        throw new RuntimeException("La sala no existe.");
                    }

                    String hostUid = roomSnapshot.getString("hostUid");
                    String status = roomSnapshot.getString("status");
                    Long playerCountValue = roomSnapshot.getLong("playerCount");

                    int playerCount;
                    if (playerCountValue != null) {
                        playerCount = playerCountValue.intValue();
                    } else {
                        playerCount = 0;
                    }

                    if (hostUid == null || !hostUid.equals(uid)) {
                        throw new RuntimeException("Solo el host puede iniciar la partida.");
                    }

                    boolean validStatus = false;
                    if (RoomStatus.OPEN.equals(status) || RoomStatus.FULL.equals(status)) {
                        validStatus = true;
                    }

                    if (!validStatus) {
                        throw new RuntimeException("La sala no está lista para iniciar.");
                    }

                    if (playerCount < 2) {
                        throw new RuntimeException("No hay jugadores suficientes.");
                    }

                    Map<String, Object> roomUpdates = new HashMap<>();
                    roomUpdates.put("status", RoomStatus.IN_GAME);
                    roomUpdates.put("startedAt", FieldValue.serverTimestamp());

                    transaction.update(roomRef, roomUpdates);
                    return null;
                })
                .addOnSuccessListener(unused -> callback.onSuccess())
                .addOnFailureListener(e -> {
                    String errorMessage;

                    if (e.getMessage() != null) {
                        errorMessage = e.getMessage();
                    } else {
                        errorMessage = "Error al iniciar la partida.";
                    }

                    callback.onError(errorMessage);
                });
    }

    public ListenerRegistration listenAvailableRooms(LoadRoomsCallback callback) {

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

                            Long playerCount = doc.getLong("playerCount");
                            Long maxPlayers = doc.getLong("maxPlayers");

                            int safePlayerCount;
                            if (playerCount != null) {
                                safePlayerCount = playerCount.intValue();
                            } else {
                                safePlayerCount = 0;
                            }

                            int safeMaxPlayers;
                            if (maxPlayers != null) {
                                safeMaxPlayers = maxPlayers.intValue();
                            } else {
                                safeMaxPlayers = 0;
                            }

                            String safeName;
                            if (name != null && !name.trim().isEmpty()) {
                                safeName = name.trim();
                            } else {
                                safeName = "Sala sin nombre";
                            }

                            String safeCode;
                            if (code != null) {
                                safeCode = code;
                            } else {
                                safeCode = "";
                            }

                            String safeStatus;
                            if (status != null) {
                                safeStatus = status;
                            } else {
                                safeStatus = "unknown";
                            }

                            if (safeMaxPlayers > 0 && safePlayerCount >= safeMaxPlayers) {
                                continue;
                            }

                            RoomSummary summary = new RoomSummary(
                                    doc.getId(),
                                    safeName,
                                    safeCode,
                                    safeStatus,
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

                    boolean leavingUserWasHost = false;
                    if (hostUid != null && hostUid.equals(uid)) {
                        leavingUserWasHost = true;
                    }

                    Long countFromDb = roomSnapshot.getLong("playerCount");

                    int currentPlayerCount;
                    if (countFromDb != null && countFromDb >= 1L) {
                        currentPlayerCount = countFromDb.intValue();
                    } else {
                        currentPlayerCount = 1;
                    }

                    int remainingPlayers = currentPlayerCount - 1;

                    if (remainingPlayers < 0) {
                        remainingPlayers = 0;
                    }

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

    private void leaveAsNormalPlayer(DocumentReference roomRef,
                                     DocumentReference leavingMemberRef,
                                     int remainingPlayers,
                                     LeaveRoomCallback callback) {

        roomRef.get()
                .addOnSuccessListener(roomSnapshot -> {

                    int maxPlayers;

                    Long maxPlayersValue = roomSnapshot.getLong("maxPlayers");
                    if (maxPlayersValue != null) {
                        maxPlayers = maxPlayersValue.intValue();
                    } else {
                        maxPlayers = 4;
                    }

                    WriteBatch batch = db.batch();
                    batch.delete(leavingMemberRef);

                    // Si no queda nadie, borramos la sala completa.
                    if (remainingPlayers == 0) {
                        batch.delete(roomRef);
                    } else {
                        batch.update(roomRef, "playerCount", remainingPlayers);

                        String newStatus;

                        // Ajustamos el estado según los jugadores que quedan.
                        if (remainingPlayers >= maxPlayers) {
                            newStatus = RoomStatus.FULL;
                        } else {
                            newStatus = RoomStatus.OPEN;
                        }

                        batch.update(roomRef, "status", newStatus);
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

    // Si sale el host, hay que elegir a otro jugador como nuevo anfitrión.
    private void leaveAsHost(DocumentReference roomRef,
                             CollectionReference membersRef,
                             DocumentReference leavingMemberRef,
                             String leavingUid,
                             int remainingPlayers,
                             LeaveRoomCallback callback) {

        membersRef.orderBy("joinedAt", Query.Direction.ASCENDING)
                .get()
                .addOnSuccessListener(memberSnapshots -> {

                    String tempHostUid = null;

                    // Elegimos como nuevo host al primer jugador que siga en la sala.
                    for (DocumentSnapshot doc : memberSnapshots.getDocuments()) {
                        String candidateUid = doc.getId();

                        if (!candidateUid.equals(leavingUid)) {
                            tempHostUid = candidateUid;
                            break;
                        }
                    }

                    final String newHostUid = tempHostUid;

                    roomRef.get()
                            .addOnSuccessListener(roomSnapshot -> {

                                int maxPlayers;

                                Long maxPlayersValue = roomSnapshot.getLong("maxPlayers");
                                if (maxPlayersValue != null) {
                                    maxPlayers = maxPlayersValue.intValue();
                                } else {
                                    maxPlayers = 4;
                                }

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

                                    DocumentReference newHostRef = membersRef.document(newHostUid);
                                    batch.update(newHostRef, "isHost", true);

                                    String newStatus;

                                    if (remainingPlayers >= maxPlayers) {
                                        newStatus = RoomStatus.FULL;
                                    } else {
                                        newStatus = RoomStatus.OPEN;
                                    }

                                    batch.update(roomRef, "status", newStatus);
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

    public void updatePresence(String roomId, String userId) {
        roomsRef.document(roomId)
                .collection("members")
                .document(userId)
                .update("lastSeenAt", FieldValue.serverTimestamp());
    }

    // Recalculamos el contador y el estado de la sala según los miembros reales.
    public void recalculateRoomState(String roomId) {

        if (roomId == null || roomId.trim().isEmpty()) {
            return;
        }

        DocumentReference roomRef = roomsRef.document(roomId);

        roomRef.get()
                .addOnSuccessListener(roomSnapshot -> {

                    if (roomSnapshot == null || !roomSnapshot.exists()) {
                        return;
                    }

                    Long maxPlayersValue = roomSnapshot.getLong("maxPlayers");

                    int maxPlayers;
                    if (maxPlayersValue != null) {
                        maxPlayers = maxPlayersValue.intValue();
                    } else {
                        maxPlayers = 4;
                    }

                    roomRef.collection("members")
                            .get()
                            .addOnSuccessListener(memberSnapshots -> {

                                int realCount = memberSnapshots.size();

                                if (realCount == 0) {
                                    roomRef.delete();
                                    return;
                                }

                                String newStatus;

                                if (realCount >= maxPlayers) {
                                    newStatus = RoomStatus.FULL;
                                } else {
                                    newStatus = RoomStatus.OPEN;
                                }

                                Map<String, Object> updates = new HashMap<>();
                                updates.put("playerCount", realCount);
                                updates.put("status", newStatus);

                                roomRef.update(updates);
                            });
                });
    }

    // Eliminamos miembros que llevan demasiado tiempo sin actividad.
    public void cleanupInactiveMembers(String roomId) {

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

                        com.google.firebase.Timestamp lastSeenAt = doc.getTimestamp("lastSeenAt");

                        if (lastSeenAt != null) {

                            long lastSeenMillis = lastSeenAt.toDate().getTime();
                            long diff = now - lastSeenMillis;

                            // Si el usuario supera el tiempo máximo de inactividad, lo eliminamos.
                            if (diff > timeoutMillis) {
                                batch.delete(doc.getReference());
                                hasChanges = true;
                            }
                        }
                    }

                    // Si hubo cambios, aplicamos y recalculamos la sala.
                    if (hasChanges) {
                        batch.commit()
                                .addOnSuccessListener(unused -> recalculateRoomState(roomId));
                    }
                });
    }

    public void loadAvailableCategories(LoadCategoriesCallback callback) {

        db.collection("preguntas")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {

                    List<String> categories = new ArrayList<>();
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