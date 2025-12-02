package com.example.ut2_app.adapters

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.ut2_app.databinding.ItemEjercicioBinding
import com.example.ut2_app.model.Ejercicio

class EjercicioAdapter(
    private var ejercicios: List<Ejercicio>
) : RecyclerView.Adapter<EjercicioAdapter.EjercicioViewHolder>() {

    inner class EjercicioViewHolder(val binding: ItemEjercicioBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(ejercicio: Ejercicio) {
            Log.d("EjercicioAdapter", "Mostrando: ${ejercicio.nombre}, series=${ejercicio.series.size}")

            // Mostrar nombre del ejercicio
            binding.textViewNombreEjercicio.text = ejercicio.nombre

            if (ejercicio.series.isNotEmpty()) {
                val resumen = buildString {
                    append("${ejercicio.series.size} ")
                    append(if (ejercicio.series.size == 1) "serie" else "series")
                    append(" de ")

                    ejercicio.series.forEachIndexed { index, serie ->
                        when (index) {
                            0 -> {
                                // Primera serie
                                append("${serie.repeticiones} reps y ${formatPeso(serie.peso)} kg")
                            }
                            ejercicio.series.size - 1 -> {
                                // Última serie
                                append(" y ${serie.repeticiones} reps y ${formatPeso(serie.peso)} kg la ${numeroATexto(index + 1)}")
                            }
                            else -> {
                                // Series intermedias
                                append(", ${serie.repeticiones} reps y ${formatPeso(serie.peso)} kg")
                            }
                        }
                    }

                    // Solo añadir "la primera/segunda" si hay 2 series
                    if (ejercicio.series.size == 2) {
                        // Ya está manejado arriba
                    } else if (ejercicio.series.size > 2) {
                        // Para más de 2 series, no usar texto descriptivo
                    } else if (ejercicio.series.size == 1) {
                        // Una sola serie, no necesita descripción adicional
                    }
                }

                binding.textViewResumenSeries.text = resumen
                binding.textViewResumenSeries.visibility = View.VISIBLE

                Log.d("EjercicioAdapter", "Resumen: $resumen")
            } else {
                // Si no hay series individuales guardadas, mostrar totales
                val resumen = "Total: ${ejercicio.reps} reps • Peso: ${formatPeso(ejercicio.peso)} kg"
                binding.textViewResumenSeries.text = resumen
                binding.textViewResumenSeries.visibility = View.VISIBLE

                Log.d("EjercicioAdapter", "Sin series, mostrando totales")
            }
        }

        /**
         * Formatea el peso para mostrar sin decimales si es entero.
         */
        private fun formatPeso(peso: Double): String {
            return if (peso % 1.0 == 0.0) {
                peso.toInt().toString()
            } else {
                String.format("%.1f", peso)
            }
        }

        /**
         * Convierte número a texto ordinal.
         */
        private fun numeroATexto(numero: Int): String {
            return when (numero) {
                1 -> "primera"
                2 -> "segunda"
                3 -> "tercera"
                4 -> "cuarta"
                5 -> "quinta"
                6 -> "sexta"
                7 -> "séptima"
                8 -> "octava"
                9 -> "novena"
                10 -> "décima"
                else -> "${numero}ª"
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

    /**
     * Actualiza la lista de ejercicios y notifica al RecyclerView.
     */
    fun actualizarLista(nuevaLista: List<Ejercicio>) {
        this.ejercicios = nuevaLista
        notifyDataSetChanged()
    }
}