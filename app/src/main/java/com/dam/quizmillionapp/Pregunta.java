package com.dam.quizmillionapp;

import java.util.ArrayList;
import java.util.List;

public class Pregunta {
    public int id;
    public String enunciado;
    public String imagen;
    public int correcta;
    public List<String> opciones;
    public int nivel;

    // Usamos List<Integer> porque Firestore mapea los Arrays de números así
    public List<Integer> comodin_50 = new ArrayList<>();
    public int comodin_llamada;
    public int comodin_publico;

    // OBLIGATORIO: Constructor vacío
    public Pregunta() {}

    public Pregunta(String enunciado, String imagen, int correcta, List<String> opciones, int nivel) {
        this.enunciado = enunciado;
        this.imagen = imagen;
        this.correcta = correcta;
        this.opciones = opciones;
        this.nivel = nivel;
    }
}
