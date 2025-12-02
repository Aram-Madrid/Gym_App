package com.example.ut2_app.model

import java.io.Serializable

data class Ejercicio(
    val idDato: String,
    val nombre: String,
    val reps: Int,
    val peso: Double,
    val dificultad: Double,

    // Lista de series para el detalle del Adapter
    val series: List<Serie> = emptyList()
): Serializable