package com.example.ut2_app.model

/**
 * Clase sellada para manejar diferentes tipos de vistas en el RecyclerView.
 * Puede ser una Cabecera (Semana X) o un Día de Rutina.
 */
sealed class RutinaDisplayItem {

    data class CabeceraSemana(
        val titulo: String,
        val rangoFechas: String
    ) : RutinaDisplayItem()

    data class ItemDia(
        val dia: DiaSemanaUI
    ) : RutinaDisplayItem()
}