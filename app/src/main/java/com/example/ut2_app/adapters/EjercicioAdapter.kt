package com.example.ut2_app.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.ut2_app.databinding.ItemEjercicioBinding
import com.example.ut2_app.model.Ejercicio

class EjercicioAdapter(
    private val ejercicios: List<Ejercicio>
) : RecyclerView.Adapter<EjercicioAdapter.EjercicioViewHolder>() {

    inner class EjercicioViewHolder(val binding: ItemEjercicioBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(ejercicio: Ejercicio) {
            binding.textViewNombreEjercicio.text = ejercicio.nombre

            // Mostrar resumen de series si existen
            if (ejercicio.series.isNotEmpty()) {
                val resumen = ejercicio.series.mapIndexed { index, serie ->
                    "Serie ${index + 1}: ${serie.peso} kg x ${serie.repeticiones}"
                }.joinToString("\n")

                binding.textViewResumenSeries.text = resumen
                binding.textViewResumenSeries.visibility = View.VISIBLE
            } else {
                binding.textViewResumenSeries.text = "Sin series registradas"
                binding.textViewResumenSeries.visibility = View.VISIBLE
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EjercicioViewHolder {
        val binding = ItemEjercicioBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return EjercicioViewHolder(binding)
    }

    override fun onBindViewHolder(holder: EjercicioViewHolder, position: Int) {
        holder.bind(ejercicios[position])
    }

    override fun getItemCount(): Int = ejercicios.size
}
