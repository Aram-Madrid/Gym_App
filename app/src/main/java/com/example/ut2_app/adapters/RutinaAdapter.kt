package com.example.ut2_app.adapters

import android.content.Intent
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.example.ut2_app.R
import com.example.ut2_app.activities.EjercicioActivity
import com.example.ut2_app.databinding.ItemRutinaBinding
import com.example.ut2_app.model.RutinaDisplayItem
import kotlinx.coroutines.launch

class RutinaAdapter(
    private var listaItems: List<RutinaDisplayItem>,
    private val lifecycleOwner: LifecycleOwner,
    private val onCrearRutina: suspend (String, String) -> String?
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val TIPO_CABECERA = 0
        private const val TIPO_ITEM = 1
    }

    inner class CabeceraViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val txtTitulo: TextView = itemView.findViewById(R.id.tvTituloSemana)
        val txtSubtitulo: TextView = itemView.findViewById(R.id.tvRangoFechas)

        fun bind(header: RutinaDisplayItem.CabeceraSemana) {
            txtTitulo.text = header.titulo
            txtSubtitulo.text = header.rangoFechas
        }
    }

    inner class RutinaViewHolder(val binding: ItemRutinaBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: RutinaDisplayItem.ItemDia) {
            val dia = item.dia
            binding.textViewDia.text = dia.nombreDia

            val colorFondo = if (dia.isActive) {
                Color.parseColor("#4CAF50") // Verde
            } else {
                Color.parseColor("#BDBDBD") // Gris
            }
            binding.root.setBackgroundColor(colorFondo)

            binding.root.setOnClickListener {
                val context = binding.root.context
                lifecycleOwner.lifecycleScope.launch {
                    try {
                        val idDiaFinal = if (dia.idDiaRutina == null) {
                            Toast.makeText(context, "Creando rutina para ${dia.fechaObjetivo}...", Toast.LENGTH_SHORT).show()
                            onCrearRutina(dia.nombreDia, dia.fechaObjetivo)
                        } else {
                            dia.idDiaRutina
                        }

                        if (idDiaFinal != null) {
                            val intent = Intent(context, EjercicioActivity::class.java).apply {
                                putExtra("id_dia", idDiaFinal)
                                putExtra("nombre_dia", dia.nombreDia)
                            }
                            context.startActivity(intent)
                        }
                    } catch (e: Exception) {
                        Toast.makeText(context, "Error: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    override fun getItemViewType(position: Int): Int {
        return when (listaItems[position]) {
            is RutinaDisplayItem.CabeceraSemana -> TIPO_CABECERA
            is RutinaDisplayItem.ItemDia -> TIPO_ITEM
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == TIPO_CABECERA) {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_rutina_header, parent, false)
            CabeceraViewHolder(view)
        } else {
            val binding = ItemRutinaBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
            RutinaViewHolder(binding)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = listaItems[position]) {
            is RutinaDisplayItem.CabeceraSemana -> (holder as CabeceraViewHolder).bind(item)
            is RutinaDisplayItem.ItemDia -> (holder as RutinaViewHolder).bind(item)
        }
    }

    override fun getItemCount(): Int = listaItems.size

    fun actualizarLista(nuevaLista: List<RutinaDisplayItem>) {
        this.listaItems = nuevaLista
        notifyDataSetChanged()
    }
}