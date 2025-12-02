package com.example.ut2_app.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

@Serializable
data class Maximos(
    @SerialName("puntos_max")
    val puntosMax: Double
)