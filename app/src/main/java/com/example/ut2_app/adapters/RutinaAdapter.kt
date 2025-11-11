package com.example.ut2_app.adapters

import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.ut2_app.activities.EjercicioActivity
import com.example.ut2_app.databinding.ItemRutinaBinding
import com.example.ut2_app.model.Rutina

class RutinaAdapter(
    private val rutinas: List<Rutina>
) : RecyclerView.Adapter<RutinaAdapter.RutinaViewHolder>() {

    inner class RutinaViewHolder(val binding: ItemRutinaBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RutinaViewHolder {
        val binding = ItemRutinaBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return RutinaViewHolder(binding)
    }

    override fun onBindViewHolder(holder: RutinaViewHolder, position: Int) {
        val rutina = rutinas[position]
        holder.binding.textViewDia.text = rutina.dia

        // Al hacer clic en una rutina, se abre la actividad de ejercicios
        holder.itemView.setOnClickListener {
            val context = holder.itemView.context
            val intent = Intent(context, EjercicioActivity::class.java)
            intent.putExtra("nombre_dia", rutina.dia) // Enviamos el día seleccionado
            context.startActivity(intent)
        }
    }

    override fun getItemCount() = rutinas.size
}
