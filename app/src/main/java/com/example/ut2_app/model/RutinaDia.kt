package com.example.ut2_app.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

/**
 * Mapea la tabla 'public.rutina_dia'.
 * Es el modelo de datos de un solo día de la rutina.
 */
@Serializable
data class RutinaDia(
    // Clave primaria (UUID)
    @SerialName("id_dia")
    val idDia: String,

    // 🔑 CLAVE: Clave foránea a Rutina (UUID). Es obligatoria para la deserialización.
    @SerialName("id_rutina")
    val idRutina: String,

    @SerialName("dia_nombre")
    val diaNombre: String,

    // El valor es numeric, por lo que usamos Double (y es nullable)
    @SerialName("puntos_total")
    val puntosTotal: Double? = 0.0
)