package com.example.ut2_app.model
import kotlinx.serialization.Serializable

@Serializable
data class Usuario(
    val id: String,
    val nombre: String,
    val email: String,
    val altura: Int? = null,
    val peso: Int? = null,
    val elo: Int = 0,
    val rango: String = "Bronze",
    val fotoPerfilUrl: String = ""
)
