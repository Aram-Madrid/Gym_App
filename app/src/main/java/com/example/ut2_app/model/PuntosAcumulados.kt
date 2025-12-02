package com.example.ut2_app.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

@Serializable
data class PuntosAcumulados(
    @SerialName("grupo")
    val grupo: String,

    @SerialName("puntos_acumulados")
    val puntosAcumulados: Double,

    @SerialName("grupo_muscular_max")
    val grupoMuscularMax: Maximos
)