package com.example.ut2_app.adapters

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.ut2_app.databinding.ItemEjercicioBinding
import com.example.ut2_app.model.Ejercicio

class EjercicioAdapter(
    // 🔑 CORRECCIÓN 1: Cambiamos a 'var' para poder reasignar la lista (incluso si es inmutable)
    // Usaremos una lista mutable internamente para la actualización
    private var ejercicios: List<Ejercicio>
) : RecyclerView.Adapter<EjercicioAdapter.EjercicioViewHolder>() {

    inner class EjercicioViewHolder(val binding: ItemEjercicioBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(ejercicio: Ejercicio) {
            Log.d("EjercicioAdapter", "Mostrando ejercicio: ${ejercicio.nombre}, reps=${ejercicio.reps}, peso=${ejercicio.peso}")

            // Mostrar nombre del ejercicio
            binding.textViewNombreEjercicio.text = ejercicio.nombre

            // 🔑 Mostrar resumen: Reps totales y Peso
            val resumen = buildString {
                append("Total: ${ejercicio.reps} reps")
                if (ejercicio.peso > 0) {
                    append(" • Peso: ${String.format("%.1f", ejercicio.peso)} kg")
                }
                if (ejercicio.dificultad > 0) {
                    append(" • Dificultad: ${String.format("%.1f", ejercicio.dificultad)}")
                }
            }

            binding.textViewResumenSeries.text = resumen
            binding.textViewResumenSeries.visibility = View.VISIBLE

            Log.d("EjercicioAdapter", "Resumen mostrado: $resumen")
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

    // 🔑 CORRECCIÓN 2: Método actualizarLista para el LiveData del ViewModel
    /**
     * Reemplaza la lista actual de ejercicios con la nueva lista del ViewModel (Supabase).
     * @param nuevaLista La lista de Ejercicio obtenida del ViewModel.
     */
    fun actualizarLista(nuevaLista: List<Ejercicio>) {
        // Asignamos la nueva lista.
        // Es más eficiente reasignar la referencia que clonar y limpiar si es una lista grande.
        this.ejercicios = nuevaLista

        // Notificamos al RecyclerView el cambio completo del dataset.
        notifyDataSetChanged()
    }
}