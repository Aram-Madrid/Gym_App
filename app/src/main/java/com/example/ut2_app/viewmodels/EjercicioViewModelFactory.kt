package com.example.ut2_app.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

// 🔑 CORRECCIÓN: El constructor debe aceptar String?
class EjercicioViewModelFactory(private val idDiaRutina: String) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(EjercicioViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            // Pasamos el String? al constructor del ViewModel
            return EjercicioViewModel(idDiaRutina) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}