package com.example.ut2_app.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

@Serializable
data class UsuarioRankingDB(
    val id: String, // UUID
    val nombre: String,

    @SerialName("elo")
    val elo: Short, // Mapea smallint

    @SerialName("rango")
    val rango: String? = "Cobre",

    @SerialName("fotoperfilurl")
    val fotoUrl: String? = null,

    // Propiedades de UI (no vienen de BD)
    val esActual: Boolean = false,
    var posicion: Int = 0
)