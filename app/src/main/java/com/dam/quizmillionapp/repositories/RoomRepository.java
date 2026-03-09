package com.dam.quizmillionapp.repositories;

import com.dam.quizmillionapp.interfaces.CreateRoomCallback;
import com.dam.quizmillionapp.interfaces.JoinRoomCallback;
import com.dam.quizmillionapp.interfaces.LeaveRoomCallback;
import com.dam.quizmillionapp.interfaces.LoadMembersCallback;
import com.dam.quizmillionapp.interfaces.LoadRoomDetailsCallback;
import com.dam.quizmillionapp.interfaces.LoadRoomsCallback;
import com.dam.quizmillionapp.interfaces.StartGameCallback;
import com.dam.quizmillionapp.models.RoomSummary;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class RoomRepository {
    private final FirebaseFirestore firestore;

    public RoomRepository() {
        firestore = FirebaseFirestore.getInstance();
    }

    //Este metodo escucha en tiempo real las salas disponibles del lobby.
    public ListenerRegistration listenAvailableRooms(final LoadRoomsCallback callback) {
        return firestore.collection("rooms")
                .whereEqualTo("status", "waiting")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .addSnapshotListener((queryResult, error) -> {

                    if (error != null) {
                        if (callback != null) {
                            callback.onError(error.getMessage());
                        }
                        return;
                    }

                    if (queryResult == null) {
                        if (callback != null) {
                            callback.onRoomsLoaded(new ArrayList<RoomSummary>());
                        }
                        return;
                    }

                    List<RoomSummary> roomList = new ArrayList<>();

                    for (DocumentSnapshot roomDocument : queryResult.getDocuments()) {
                        String roomId = roomDocument.getId();
                        String code = roomDocument.getString("code");
                        String status = roomDocument.getString("status");
                        String hostUid = roomDocument.getString("hostUid");

                        Long playerCount = roomDocument.getLong("playerCount");
                        Long maxPlayers = roomDocument.getLong("maxPlayers");

                        long safePlayerCount = playerCount == null ? 0 : playerCount;
                        long safeMaxPlayers = maxPlayers == null ? 4 : maxPlayers;

                        roomList.add(new RoomSummary(
                                roomId,
                                code,
                                status,
                                safePlayerCount,
                                safeMaxPlayers,
                                hostUid
                        ));
                    }

                    if (callback != null) {
                        callback.onRoomsLoaded(roomList);
                    }
                });
    }

    /**
     * Metodo que crea una nueva sala y añade al host como primer miembro.
     */
    public void createRoom(final String roomName, final String uid, final String displayName, final CreateRoomCallback callback) {

        if (uid == null || uid.trim().isEmpty()) {
            if (callback != null) {
                callback.onError("Usuario no disponible. Login obligatorio.");
            }
            return;
        }

        String safeDisplayName = displayName;
        if (safeDisplayName == null || safeDisplayName.trim().isEmpty()) {
            safeDisplayName = "Jugador";
        }

        final String code = generateRoomCode();
        final DocumentReference roomDoc = firestore.collection("rooms").document();

        Map<String, Object> roomData = new HashMap<>();
        roomData.put("code", code);
        roomData.put("name", roomName);
        roomData.put("hostUid", uid);
        roomData.put("status", "waiting");
        roomData.put("maxPlayers", 4);
        roomData.put("playerCount", 1);
        roomData.put("createdAt", FieldValue.serverTimestamp());

        DocumentReference memberDoc = roomDoc.collection("members").document(uid);

        Map<String, Object> memberData = new HashMap<>();
        memberData.put("displayName", safeDisplayName);
        memberData.put("joinedAt", FieldValue.serverTimestamp());
        memberData.put("score", 0);
        memberData.put("isHost", true);
        memberData.put("isReady", false);

        WriteBatch batch = firestore.batch();
        batch.set(roomDoc, roomData);
        batch.set(memberDoc, memberData);

        batch.commit()
                .addOnSuccessListener(unused -> {
                    if (callback != null) {
                        callback.onSuccess(roomDoc.getId());
                    }
                })
                .addOnFailureListener(e -> {
                    if (callback != null) {
                        callback.onError("Error al crear la sala: " + e.getMessage());
                    }
                });
    }

     // Lleva al usuario actual a una sala concreta por roomId.
    public void joinRoomByRoomId(final String roomId, final String currentUid, final String currentDisplayName, final JoinRoomCallback callback) {

        if (currentUid == null || currentUid.trim().isEmpty()) {
            if (callback != null) {
                callback.onError("Login requerido.");
            }
            return;
        }

        final String safeDisplayName;
        if (currentDisplayName == null || currentDisplayName.trim().isEmpty()) {
            safeDisplayName = "Jugador";
        } else {
            safeDisplayName = currentDisplayName;
        }

        final DocumentReference roomRef = firestore.collection("rooms").document(roomId);
        final DocumentReference memberRef = roomRef.collection("members").document(currentUid);

        firestore.runTransaction(transaction -> {
                    DocumentSnapshot roomDocument = transaction.get(roomRef);
                    DocumentSnapshot memberDocument = transaction.get(memberRef);

                    if (!roomDocument.exists()) {
                        throw new RuntimeException("Sala no encontrada");
                    }

                    String status = roomDocument.getString("status");
                    Long playerCount = roomDocument.getLong("playerCount");
                    Long maxPlayers = roomDocument.getLong("maxPlayers");

                    long safePlayerCount = playerCount == null ? 0 : playerCount;
                    long safeMaxPlayers = maxPlayers == null ? 4 : maxPlayers;

                    if (!"waiting".equals(status)) {
                        throw new RuntimeException("Sala no disponible");
                    }

                    if (memberDocument.exists()) {
                        return true;
                    }

                    if (safePlayerCount >= safeMaxPlayers) {
                        throw new RuntimeException("La sala ya está llena");
                    }

                    Map<String, Object> memberData = new HashMap<>();
                    memberData.put("displayName", safeDisplayName);
                    memberData.put("joinedAt", FieldValue.serverTimestamp());
                    memberData.put("score", 0);
                    memberData.put("isHost", false);
                    memberData.put("isReady", false);

                    transaction.set(memberRef, memberData);
                    transaction.update(roomRef, "playerCount", safePlayerCount + 1);

                    return false;
                })
                .addOnSuccessListener(alreadyJoined -> {
                    Boolean wasAlreadyMember = (Boolean) alreadyJoined;
                    if (callback != null) {
                        callback.onSuccess(roomId, Boolean.TRUE.equals(wasAlreadyMember));
                    }
                })
                .addOnFailureListener(e -> {
                    if (callback != null) {
                        callback.onError(e.getMessage());
                    }
                });
    }

    // Busca una sala por su código. Despues reutiliza la lógica de joinRoomByRoomId.
    public void joinRoomByCode(final String roomCode, final String currentUid, final String currentDisplayName, final JoinRoomCallback callback) {

        firestore.collection("rooms")
                .whereEqualTo("code", roomCode)
                .limit(1)
                .get()
                .addOnSuccessListener(queryResult -> {
                    if (queryResult.isEmpty()) {
                        if (callback != null) {
                            callback.onError("Código de sala no encontrado.");
                        }
                        return;
                    }

                    DocumentSnapshot roomDocument = queryResult.getDocuments().get(0);
                    String roomId = roomDocument.getId();

                    joinRoomByRoomId(roomId, currentUid, currentDisplayName, callback);
                })
                .addOnFailureListener(e -> {
                    if (callback != null) {
                        callback.onError("Error al buscar la sala: " + e.getMessage());
                    }
                });
    }

     // Escucha en tiempo real el documento principal de la sala.
    public ListenerRegistration listenRoomDetails(final String roomId, final LoadRoomDetailsCallback callback) {

        DocumentReference roomRef = firestore.collection("rooms").document(roomId);

        return roomRef.addSnapshotListener((snapshot, error) -> {
            if (error != null) {
                if (callback != null) {
                    callback.onError("Room listener error: " + error.getMessage());
                }
                return;
            }

            if (snapshot == null || !snapshot.exists()) {
                if (callback != null) {
                    callback.onRoomNotFound();
                }
                return;
            }

            String code = snapshot.getString("code");
            String status = snapshot.getString("status");
            String hostUid = snapshot.getString("hostUid");

            if (callback != null) {
                callback.onRoomLoaded(code, status, hostUid);
            }
        });
    }

     // Escucha en tiempo real la subcolección de miembros de una sala.
    public ListenerRegistration listenRoomMembers(final String roomId, final LoadMembersCallback callback) {

        DocumentReference roomRef = firestore.collection("rooms").document(roomId);

        return roomRef.collection("members")
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null) {
                        if (callback != null) {
                            callback.onError("Members listener error: " + error.getMessage());
                        }
                        return;
                    }

                    if (snapshot == null) {
                        if (callback != null) {
                            callback.onMembersLoaded(new ArrayList<String>());
                        }
                        return;
                    }

                    List<String> names = new ArrayList<>();

                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        String name = doc.getString("displayName");
                        if (name == null || name.trim().isEmpty()) {
                            name = doc.getId();
                        }
                        names.add(name);
                    }

                    if (callback != null) {
                        callback.onMembersLoaded(names);
                    }
                });
    }

     // Se inicia la partida si el usuario actual es el host.
    public void startGameIfHost(final String roomId, final String myUid, final StartGameCallback callback) {

        if (myUid == null || myUid.trim().isEmpty()) {
            if (callback != null) {
                callback.onError("Usuario no disponible");
            }
            return;
        }

        DocumentReference roomRef = firestore.collection("rooms").document(roomId);

        roomRef.get()
                .addOnSuccessListener(snapshot -> {
                    if (!snapshot.exists()) {
                        if (callback != null) {
                            callback.onError("Sala no encontrada");
                        }
                        return;
                    }

                    String hostUid = snapshot.getString("hostUid");
                    if (hostUid == null || !hostUid.equals(myUid)) {
                        if (callback != null) {
                            callback.onError("Solo el anfitrión puede iniciar la partida");
                        }
                        return;
                    }

                    Map<String, Object> updates = new HashMap<>();
                    updates.put("status", "in_progress");
                    updates.put("startedAt", FieldValue.serverTimestamp());

                    roomRef.update(updates)
                            .addOnSuccessListener(unused -> {
                                if (callback != null) {
                                    callback.onSuccess();
                                }
                            })
                            .addOnFailureListener(e -> {
                                if (callback != null) {
                                    callback.onError("Start failed: " + e.getMessage());
                                }
                            });
                })
                .addOnFailureListener(e -> {
                    if (callback != null) {
                        callback.onError("Start check failed: " + e.getMessage());
                    }
                });
    }


      // Si el usuario no existe en members, no se toca el contador
     // Si era el último jugador, se elimina la sala
    // Si no era el host, se elimina al miembro y se actualiza el contador
    // Si era el host y aun quedan jugadores, se transfiere el host al miembro mas antiguo
    public void leaveRoom(final String roomId, final String uid, final LeaveRoomCallback callback) {

        if (roomId == null || roomId.trim().isEmpty()) {
            if (callback != null) {
                callback.onError("RoomId no válido.");
            }
            return;
        }

        if (uid == null || uid.trim().isEmpty()) {
            if (callback != null) {
                callback.onSuccess();
            }
            return;
        }

        final DocumentReference roomRef = firestore.collection("rooms").document(roomId);
        final DocumentReference memberRef = roomRef.collection("members").document(uid);

        // Buscamos un candidato a nuevo host (por si nos hiciera falta)
        roomRef.collection("members")
                .orderBy("joinedAt", Query.Direction.ASCENDING)
                .get()
                .addOnSuccessListener(querySnapshot -> {

                    String candidateUid = null;

                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        if (!doc.getId().equals(uid)) {
                            candidateUid = doc.getId();
                            break;
                        }
                    }

                    final String newHostCandidateUid = candidateUid;

                    firestore.runTransaction(transaction -> {
                                DocumentSnapshot roomSnap = transaction.get(roomRef);
                                DocumentSnapshot leavingMemberSnap = transaction.get(memberRef);

                                if (!roomSnap.exists()) {
                                    return null;
                                }

                                if (!leavingMemberSnap.exists()) {
                                    return null;
                                }

                                Long playerCount = roomSnap.getLong("playerCount");
                                long safeCount = playerCount != null ? playerCount : 0;

                                String currentHostUid = roomSnap.getString("hostUid");
                                boolean isLeavingHost = currentHostUid != null && currentHostUid.equals(uid);

                                // Si era el último jugador, se elimina la sala
                                if (safeCount <= 1) {
                                    transaction.delete(memberRef);
                                    transaction.delete(roomRef);
                                    return null;
                                }

                                // Si no era el host, se elimina al miembro y se actualiza el contador
                                if (!isLeavingHost) {
                                    transaction.delete(memberRef);
                                    transaction.update(roomRef, "playerCount", safeCount - 1);
                                    return null;
                                }

                                // Si era el host y aun quedan jugadores, se transfiere el host al miembro mas antiguo
                                if (newHostCandidateUid == null || newHostCandidateUid.trim().isEmpty()) {
                                    throw new RuntimeException("Host transfer failed");
                                }

                                DocumentReference newHostRef = roomRef.collection("members").document(newHostCandidateUid);
                                DocumentSnapshot newHostSnap = transaction.get(newHostRef);

                                if (!newHostSnap.exists()) {
                                    throw new RuntimeException("Host transfer failed: candidate member not found.");
                                }

                                transaction.delete(memberRef);
                                transaction.update(roomRef, "playerCount", safeCount - 1);
                                transaction.update(roomRef, "hostUid", newHostCandidateUid);
                                transaction.update(newHostRef, "isHost", true);

                                return null;
                            })
                            .addOnSuccessListener(unused -> {
                                if (callback != null) {
                                    callback.onSuccess();
                                }
                            })
                            .addOnFailureListener(e -> {
                                if (callback != null) {
                                    callback.onError("Leave failed: " + e.getMessage());
                                }
                            });
                })
                .addOnFailureListener(e -> {
                    if (callback != null) {
                        callback.onError("Error al buscar nuevo anfitrión: " + e.getMessage());
                    }
                });

    }

     // Generar un código aleatorio de 6 caracteres para asignarlo a la sala.
    private String generateRoomCode() {
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
        StringBuilder sb = new StringBuilder();
        Random random = new Random();

        for (int i = 0; i < 6; i++) {
            int index = random.nextInt(chars.length());
            sb.append(chars.charAt(index));
        }

        return sb.toString();
    }
}