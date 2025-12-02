package com.example.ut2_app.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

/**
 * Factory para crear EjercicioViewModel con un ID de día específico.
 * El ID puede ser null cuando se está creando una rutina nueva.
 */
class EjercicioViewModelFactory(
    private val idDiaRutina: String? // ✅ Acepta null
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(EjercicioViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return EjercicioViewModel(idDiaRutina) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}