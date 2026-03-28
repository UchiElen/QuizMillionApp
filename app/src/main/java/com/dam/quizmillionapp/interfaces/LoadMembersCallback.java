package com.dam.quizmillionapp.interfaces;

import com.dam.quizmillionapp.models.MemberListItem;
import java.util.List;

// Se usa para obtener los jugadores de una sala en tiempo real
// y actualizar la lista mostrada en la pantalla de espera.
public interface LoadMembersCallback {

    void onMembersLoaded(List<MemberListItem> members);

    void onError(String errorMessage);
}