package com.dam.quizmillionapp.interfaces;

import java.util.List;

// Se usa para cargar las categorías disponibles desde la base de datos
// y mostrarlas en la configuración de la sala.
public interface LoadCategoriesCallback {

    void onCategoriesLoaded(List<String> categories);

    void onError(String errorMessage);
}