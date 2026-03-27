package com.dam.quizmillionapp.interfaces;

import com.dam.quizmillionapp.models.MemberListItem;
import java.util.List;

// Callback para devolver la lista de jugadores de la sala.
public interface LoadMembersCallback {

    // Se llama cuando se han cargado los miembros correctamente
    void onMembersLoaded(List<MemberListItem> members);

    void onError(String errorMessage);
}