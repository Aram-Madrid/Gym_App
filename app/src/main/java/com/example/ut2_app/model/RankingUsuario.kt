package com.example.ut2_app.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

@Serializable
data class UsuarioRankingDB(
    val id: String,
    val nombre: String,

    @SerialName("elo")
    val elo: Short,

    @SerialName("rango")
    val rango: String? = "Cobre",

    @SerialName("fotoperfilurl")
    val fotoUrl: String? = null,


    val esActual: Boolean = false,
    var posicion: Int = 0
)