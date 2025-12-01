package com.example.ut2_app.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

/**
 * Modelo para decodificar la consulta con JOIN:
 * SELECT *, ejercicio(*) FROM rutina_dia_datos
 */
@Serializable
data class RutinaDiaDatosConEjercicio(
    // Columnas de rutina_dia_datos
    @SerialName("id_dato")
    val id_dato: String,
    @SerialName("id_dia")
    val id_dia: String,
    @SerialName("id_ejercicio")
    val id_ejercicio: String,

    @SerialName("reps")
    val reps: Int,
    @SerialName("peso")
    val peso: Double,
    @SerialName("dificultad")
    val dificultad: Double,

    @SerialName("ptm")
    val ptm: Double? = null,
    @SerialName("elo")
    val elo: Double? = null,

    // Resultado del JOIN anidado:
    @SerialName("ejercicio")
    val ejercicio: EjercicioDetalle
)