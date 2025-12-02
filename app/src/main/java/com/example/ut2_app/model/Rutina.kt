package com.example.ut2_app.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

/**
 * Representa la rutina principal (el grupo) y se utiliza como modelo de decodificación
 * para la respuesta con JOIN de Supabase, la cual anida los días.
 */
@Serializable
data class Rutina(
    @SerialName("id_rutina")
    val idRutina: String,

    @SerialName("id_usuario")
    val idUsuario: String,

    @SerialName("nombre_rutina")
    val nombreRutina: String?,

    // 🔑 Campo Anidado: Contiene la lista de días asociados a esta rutina,
    // que vienen de la tabla 'rutina_dia'.
    @SerialName("rutina_dia")
    val rutinaDias: List<RutinaDia> = emptyList()
)