package com.example.ut2_app.model

// Modelo para el Estado de la UI
data class PuntosGrupoUI(
    val grupo: String,
    val valor: Double, // ⬅️ Cambiado a Double
    val maximo: Double // ⬅️ Cambiado a Double
)