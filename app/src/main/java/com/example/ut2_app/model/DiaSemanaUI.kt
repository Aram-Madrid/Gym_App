package com.example.ut2_app.model

data class DiaSemanaUI(
    val nombreDia: String,
    val idDiaRutina: String?, // ID si ya existe (para editar)
    val isActive: Boolean,
    val fechaObjetivo: String // 🔑 NUEVO: Fecha exacta (YYYY-MM-DD) para crear el día
)