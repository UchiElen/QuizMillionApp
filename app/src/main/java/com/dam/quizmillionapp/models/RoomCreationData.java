
package com.dam.quizmillionapp.models;

import java.util.List;

// Guarda la configuración elegida por el usuario
// antes de crear una sala nueva.
public class RoomCreationData {
    private String name;
    private long maxPlayers;
    private boolean isPublic;
    private List<String> categories;

    public RoomCreationData() {
    }

    public RoomCreationData(String name, List<String> categories, long maxPlayers, boolean isPublic) {
        this.name = name;
        this.categories = categories;
        this.maxPlayers = maxPlayers;
        this.isPublic = isPublic;
    }

    public String getName() {

        return name;
    }

    public long getMaxPlayers() {

        return maxPlayers;
    }

    public boolean isPublic() {
        return isPublic;
    }

    public List<String> getCategories() {
        return categories;
    }
}