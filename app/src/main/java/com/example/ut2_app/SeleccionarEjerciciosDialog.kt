package com.example.ut2_app

import android.app.AlertDialog
import android.content.Context
import android.view.LayoutInflater
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.ut2_app.adapters.EjercicioAdapter
import com.example.ut2_app.databinding.DialogSeleccionarEjerciciosBinding
import com.example.ut2_app.model.Ejercicio

class SeleccionarEjerciciosDialog(
    context: Context,
    private val ejercicios: List<Ejercicio>,
    private val onConfirm: (List<Ejercicio>) -> Unit
) {

    private val binding: DialogSeleccionarEjerciciosBinding

    init {
        val inflater = LayoutInflater.from(context)
        binding = DialogSeleccionarEjerciciosBinding.inflate(inflater)

        val adapter = EjercicioAdapter(
            ejercicios,
            onClick = { ejercicio ->
                ejercicio.seleccionado = !ejercicio.seleccionado
                binding.recyclerViewDialogEjercicios.adapter?.notifyDataSetChanged()
            }
        )

        binding.recyclerViewDialogEjercicios.layoutManager = LinearLayoutManager(context)
        binding.recyclerViewDialogEjercicios.adapter = adapter

        AlertDialog.Builder(context)
            .setView(binding.root)
            .setPositiveButton("Confirmar") { _, _ ->
                val seleccionados = ejercicios.filter { it.seleccionado }
                onConfirm(seleccionados)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }
}
