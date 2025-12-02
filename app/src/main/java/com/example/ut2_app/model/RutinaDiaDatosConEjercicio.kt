package com.example.ut2_app.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

/**
 * Modelo para decodificar la consulta con JOIN:
 * SELECT *, ejercicio(*) FROM rutina_dia_datos
 *
 * IMPORTANTE: Los nombres de @SerialName deben coincidir EXACTAMENTE
 * con los nombres de columnas en la base de datos.
 */
@Serializable
data class RutinaDiaDatosConEjercicio(
    // Columnas de rutina_dia_datos
    @SerialName("id_dato")
    val id_dato: String,

    @SerialName("routine_day_id")
    val routine_day_id: String,

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