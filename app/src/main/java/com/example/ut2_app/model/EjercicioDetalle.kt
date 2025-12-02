package com.example.ut2_app.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

/**
 * Mapea la tabla 'public.ejercicio' (Catálogo de ejercicios).
 */
@Serializable
data class EjercicioDetalle(
    @SerialName("id_ejercicio")
    val id_ejercicio: String,

    @SerialName("nombre")
    val nombre: String,

    @SerialName("grupo_muscular")
    val grupo_muscular: String? = null,

    @SerialName("dificultad")
    val dificultad: Double? = null
)