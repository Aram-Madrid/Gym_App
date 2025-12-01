package com.example.ut2_app.adapters

import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.example.ut2_app.activities.EjercicioActivity
import com.example.ut2_app.databinding.ItemRutinaBinding
import com.example.ut2_app.model.DiaSemanaUI
import android.graphics.Color
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class RutinaAdapter(
    private var listaDias: List<DiaSemanaUI>,
    private val lifecycleOwner: LifecycleOwner, // 🔑 Necesario para lanzar coroutines
    private val onCrearRutina: suspend (String) -> String? // 🔑 Callback para crear rutina
) : RecyclerView.Adapter<RutinaAdapter.RutinaViewHolder>() {

    inner class RutinaViewHolder(val binding: ItemRutinaBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(dia: DiaSemanaUI) {
            // Mostrar el nombre del día
            binding.textViewDia.text = dia.nombreDia

            // 🔑 Estilo visual: Resaltar días activos
            val colorFondo = if (dia.isActive) {
                Color.parseColor("#4CAF50") // Verde para días con rutina
            } else {
                Color.parseColor("#BDBDBD") // Gris para días sin rutina
            }
            binding.root.setBackgroundColor(colorFondo)

            // 🔑 Click handler con creación automática de rutina
            binding.root.setOnClickListener {
                val context = binding.root.context

                lifecycleOwner.lifecycleScope.launch {
                    try {
                        // Obtener o crear el ID de la rutina del día
                        val idDiaFinal = if (dia.idDiaRutina == null) {
                            // 🔑 CREAR RUTINA AUTOMÁTICAMENTE
                            Toast.makeText(
                                context,
                                "Creando rutina para ${dia.nombreDia}...",
                                Toast.LENGTH_SHORT
                            ).show()

                            onCrearRutina(dia.nombreDia)
                        } else {
                            // Ya existe, usar el ID existente
                            dia.idDiaRutina
                        }

                        // Verificar que tenemos un ID válido
                        if (idDiaFinal == null) {
                            Toast.makeText(
                                context,
                                "Error al crear la rutina. Intenta de nuevo.",
                                Toast.LENGTH_LONG
                            ).show()
                            return@launch
                        }

                        // Navegar a EjercicioActivity con el ID (nuevo o existente)
                        val intent = Intent(context, EjercicioActivity::class.java).apply {
                            putExtra("id_dia", idDiaFinal)
                            putExtra("nombre_dia", dia.nombreDia)
                        }
                        context.startActivity(intent)

                    } catch (e: Exception) {
                        Toast.makeText(
                            context,
                            "Error: ${e.localizedMessage}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RutinaViewHolder {
        val binding = ItemRutinaBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return RutinaViewHolder(binding)
    }

    override fun onBindViewHolder(holder: RutinaViewHolder, position: Int) {
        holder.bind(listaDias[position])
    }

    override fun getItemCount(): Int = listaDias.size

    /**
     * Actualiza la lista de días desde el ViewModel.
     */
    fun actualizarLista(nuevaLista: List<DiaSemanaUI>) {
        this.listaDias = nuevaLista
        notifyDataSetChanged()
    }
}