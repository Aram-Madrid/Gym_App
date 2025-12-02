package com.example.ut2_app.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

/**
 * DTO para insertar datos en la tabla 'rutina_dia_datos'.
 * Los nombres deben coincidir EXACTAMENTE con las columnas de la BD.
 */
@Serializable
data class RutinaDiaDatoInsert(
    @SerialName("id_dato")
    val id_dato: String,

    // 🔑 CORRECCIÓN: La columna se llama 'routine_day_id' en la BD
    @SerialName("routine_day_id")
    val routine_day_id: String,

    @SerialName("id_ejercicio")
    val id_ejercicio: String,

    @SerialName("reps")
    val reps: Int,

    @SerialName("peso")
    val peso: Double,

    @SerialName("dificultad")
    val dificultad: Double
)