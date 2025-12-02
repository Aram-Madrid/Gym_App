package com.example.ut2_app.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

/**
 * Modelo para la tabla 'series' en Supabase.
 * Almacena cada serie individual de un ejercicio.
 */
@Serializable
data class SerieDB(
    @SerialName("id_serie")
    val idSerie: String,

    @SerialName("id_dato")
    val idDato: String, // FK a rutina_dia_datos

    @SerialName("numero_serie")
    val numeroSerie: Int,

    @SerialName("peso")
    val peso: Double,

    @SerialName("repeticiones")
    val repeticiones: Int
)

/**
 * DTO para insertar series en la BD.
 */
@Serializable
data class SerieInsert(
    @SerialName("id_serie")
    val id_serie: String,

    @SerialName("id_dato")
    val id_dato: String,

    @SerialName("numero_serie")
    val numero_serie: Int,

    @SerialName("peso")
    val peso: Double,

    @SerialName("repeticiones")
    val repeticiones: Int
)