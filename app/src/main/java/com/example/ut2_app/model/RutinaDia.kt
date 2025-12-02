package com.example.ut2_app.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

@Serializable
data class RutinaDia(
    @SerialName("id_dia")
    val idDia: String,

    @SerialName("id_rutina")
    val idRutina: String,

    @SerialName("dia_nombre")
    val diaNombre: String,

    @SerialName("puntos_total")
    val puntosTotal: Double? = 0.0,

    //Fecha de creación para agrupar por semanas
    @SerialName("created_at")
    val fecha: String? = null
)