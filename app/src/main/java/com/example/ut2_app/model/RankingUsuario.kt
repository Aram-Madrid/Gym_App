package com.example.ut2_app.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

@Serializable
data class UsuarioRankingDB( // Usaremos este nombre para la decodificación de la BD
    val id: String, // UUID
    val nombre: String,
    @SerialName("elo")
    val elo: Short, // Mapea smallint
    @SerialName("fotoperfilurl")
    val fotoUrl: String? = null,

    // Propiedades que el Adapter necesita (pero no vienen directamente de la BD)
    val esActual: Boolean = false,
    var posicion: Int = 0
)