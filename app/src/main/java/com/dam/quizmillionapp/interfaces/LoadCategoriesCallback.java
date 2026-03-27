package com.dam.quizmillionapp.interfaces;

import java.util.List;

// Callback para devolver la lista de categorías disponibles.
public interface LoadCategoriesCallback {

    // Se llama cuando las categorías se han cargado correctamente
    void onCategoriesLoaded(List<String> categories);

    void onError(String errorMessage);
}