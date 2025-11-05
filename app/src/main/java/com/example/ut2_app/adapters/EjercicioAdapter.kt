package com.example.ut2_app.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.ut2_app.databinding.ItemEjercicioBinding
import com.example.ut2_app.model.Ejercicio

class EjercicioAdapter(
    private val ejercicios: List<Ejercicio>,
    private val onClick: (Ejercicio) -> Unit
) : RecyclerView.Adapter<EjercicioAdapter.EjercicioViewHolder>() {

    inner class EjercicioViewHolder(val binding: ItemEjercicioBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(ejercicio: Ejercicio) {
            binding.textViewNombreEjercicio.text = ejercicio.nombre

            // Mostrar resumen de series si existen
            if (ejercicio.series.isNotEmpty()) {
                val resumen = ejercicio.series.mapIndexed { index, serie ->
                    "Serie ${index + 1}: ${serie.peso}kg x ${serie.repeticiones}"
                }.joinToString("\n")
                binding.textViewResumenSeries.text = resumen
                binding.textViewResumenSeries.visibility = android.view.View.VISIBLE
            } else {
                binding.textViewResumenSeries.visibility = android.view.View.GONE
            }

            // Click en todo el item
            binding.root.setOnClickListener {
                onClick(ejercicio)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EjercicioViewHolder {
        val binding = ItemEjercicioBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return EjercicioViewHolder(binding)
    }

    override fun onBindViewHolder(holder: EjercicioViewHolder, position: Int) {
        holder.bind(ejercicios[position])
    }

    override fun getItemCount(): Int = ejercicios.size
}
