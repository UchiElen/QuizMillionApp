package com.dam.quizmillionapp.interfaces;

import java.util.List;

public interface LoadCategoriesCallback {
    void onCategoriesLoaded(List<String> categories);
    void onError(String errorMessage);
}