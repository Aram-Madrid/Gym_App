package com.example.ut2_app.adapters

import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.example.ut2_app.activities.EjercicioActivity
import com.example.ut2_app.databinding.ItemRutinaBinding
import com.example.ut2_app.model.DiaSemanaUI // ⬅️ ¡IMPORTACIÓN CLAVE! Usamos el modelo combinado
import android.graphics.Color // Para cambiar colores si es necesario

class RutinaAdapter(
    // 🔑 1. SOLUCIÓN: El adaptador ahora acepta la lista combinada (DiaSemanaUI)
    private var listaDias: List<DiaSemanaUI>
) : RecyclerView.Adapter<RutinaAdapter.RutinaViewHolder>() {

    inner class RutinaViewHolder(val binding: ItemRutinaBinding) :
        RecyclerView.ViewHolder(binding.root) {

        // 🔑 2. SOLUCIÓN: bind ahora acepta el modelo DiaSemanaUI
        fun bind(dia: DiaSemanaUI) {

            // El texto es el nombre fijo del día de la semana
            binding.textViewDia.text = dia.nombreDia

            // 🔑 Estilo: Resaltar los días que tienen una rutina activa
            val colorFondo = if (dia.isActive) Color.parseColor("#4CAF50") else Color.LTGRAY
            binding.root.setBackgroundColor(colorFondo)

            binding.root.setOnClickListener {
                val context = binding.root.context

                // Obtenemos los IDs y nombres
                val idDiaAEnviar = dia.idDiaRutina // ⬅️ Será NULL si el día está inactivo
                val nombreDiaAEnviar = dia.nombreDia

                // 🔑 CORRECCIÓN CLAVE: Eliminamos la comprobación 'isActive' y navegamos siempre.
                // EjercicioActivity manejará el caso de id_dia nulo (Modo Creación).

                val intent = Intent(context, EjercicioActivity::class.java).apply {
                    // Pasamos el ID del DÍA (puede ser null)
                    putExtra("id_dia", idDiaAEnviar)
                    putExtra("nombre_dia", nombreDiaAEnviar)
                }
                context.startActivity(intent)

                // Opcional: Mostrar un Toast si el día no tiene ID (solo para feedback)
                if (idDiaAEnviar == null) {
                    Toast.makeText(
                        context,
                        "Creando rutina para el ${dia.nombreDia}...",
                        Toast.LENGTH_SHORT
                    ).show()
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

    // 🔑 onBindViewHolder usa la lista del nuevo modelo
    override fun onBindViewHolder(holder: RutinaViewHolder, position: Int) {
        holder.bind(listaDias[position])
    }

    override fun getItemCount(): Int = listaDias.size

    /**
     * Permite al ViewModel actualizar los datos con la lista de 7 días.
     */
    // 🔑 4. actualizarLista acepta List<DiaSemanaUI>
    fun actualizarLista(nuevaLista: List<DiaSemanaUI>) {
        this.listaDias = nuevaLista
        notifyDataSetChanged()
    }
}