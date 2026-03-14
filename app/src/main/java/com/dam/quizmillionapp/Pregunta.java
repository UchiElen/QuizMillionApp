package com.dam.quizmillionapp;

import java.util.ArrayList;
import java.util.List;

/**
 * Modelo de datos para las preguntas.
 * Esta clase es un para convertir automáticamente los documentos de la base de datos en objetos.
 */
public class Pregunta {

    // Atributos de la pregunta (Deben coincidir exactamente con los nombres en Firestore)
    public int id;
    public String enunciado;
    public String imagen;    // URL de Firebase Storage
    public int correcta;     // Índice de la respuesta correcta (0-3)
    public List<String> opciones; // Lista de las 4 opciones de respuesta
    public int nivel;        // Dificultad/Nivel de la pregunta (1-15)

    /* Campos para Comodines:
       Usamos List<Integer> para el 50% porque en Firebase lo definimos como Array.
       Los de llamada y público guardan el índice de la opción recomendada.
    */
    public List<Integer> comodin_50 = new ArrayList<>();
    public int comodin_llamada;
    public int comodin_publico;

    /**
     * CONSTRUCTOR VACÍO (para Firebase).
     * Hemos tenido que añadirlo porque si no, el método .toObject(Pregunta.class) lanzaría una excepción
     * ya que Firebase necesita instanciar el objeto antes de rellenar sus campos.
     */
    public Pregunta() {}

    /**
     * Constructor completo para creación manual de preguntas si fuera necesario.
     */
    public Pregunta(String enunciado, String imagen, int correcta, List<String> opciones, int nivel) {
        this.enunciado = enunciado;
        this.imagen = imagen;
        this.correcta = correcta;
        this.opciones = opciones;
        this.nivel = nivel;
    }
}
