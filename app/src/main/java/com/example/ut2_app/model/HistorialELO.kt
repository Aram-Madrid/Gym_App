package com.example.ut2_app.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

/**
 * Modelo para el historial de cambios de ELO del usuario.
 * Permite trackear cómo ha evolucionado el ELO a lo largo del tiempo.
 */
@Serializable
data class HistorialELO(
    @SerialName("id")
    val id: String,

    @SerialName("id_usuario")
    val idUsuario: String,

    @SerialName("elo_anterior")
    val eloAnterior: Double,

    @SerialName("elo_nuevo")
    val eloNuevo: Double,

    @SerialName("cambio")
    val cambio: Double,

    @SerialName("razon")
    val razon: String? = null,

    @SerialName("id_dato")
    val idDato: String? = null,

    @SerialName("created_at")
    val createdAt: String? = null
)

/**
 * Estadísticas de ELO del usuario.
 */
data class StatsELO(
    val eloActual: Int,
    val eloMaximo: Int,
    val eloMinimo: Int,
    val cambioUltimaSemana: Int,
    val cambioUltimoMes: Int,
    val totalEjercicios: Int,
    val rango: String
)