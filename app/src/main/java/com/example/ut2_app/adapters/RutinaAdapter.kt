package com.example.ut2_app.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import com.example.ut2_app.EjerciciosFragment
import com.example.ut2_app.R
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


        holder.itemView.setOnClickListener {
            val fragment = EjerciciosFragment()
            val fragmentManager = (holder.itemView.context as AppCompatActivity).supportFragmentManager
            fragmentManager.beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .addToBackStack(null)
                .commit()
        }
    }

    override fun getItemCount() = rutinas.size
}
