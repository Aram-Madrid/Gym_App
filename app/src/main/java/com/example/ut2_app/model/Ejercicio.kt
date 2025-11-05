package com.example.ut2_app.model

import java.io.Serializable

data class Serie(
    var peso: Float = 0f,
    var repeticiones: Int = 0
) : Serializable

data class Ejercicio(
    val nombre: String,
    var seleccionado: Boolean = false,
    val series: MutableList<Serie> = mutableListOf()
) : Serializable
