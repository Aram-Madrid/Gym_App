package com.example.ut2_app.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

@Serializable
data class Maximos(
    // 🔑 CORRECCIÓN: Cambiar Float a Double para manejar el tipo 'numeric'
    @SerialName("puntos_max")
    val puntosMax: Double
)