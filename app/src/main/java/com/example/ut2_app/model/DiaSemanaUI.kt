// model/DiaSemanaUI.kt
package com.example.ut2_app.model

data class DiaSemanaUI(
    val nombreDia: String,
    val idDiaRutina: String?, // ID de la rutina_dia si está activa, null si no
    val isActive: Boolean
)