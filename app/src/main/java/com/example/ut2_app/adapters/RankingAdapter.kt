package com.example.ut2_app.adapters

import android.content.Context
import android.graphics.Color
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.example.ut2_app.R
import com.example.ut2_app.model.UsuarioRankingDB

class RankingAdapter(
    private val context: Context,
    private var usuarios: List<UsuarioRankingDB>,
    // 🔑 NUEVO: Callback para el click
    private val onUserClick: (String) -> Unit
) : RecyclerView.Adapter<RankingAdapter.UsuarioViewHolder>() {

    class UsuarioViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val txtPosicion: TextView = view.findViewById(R.id.posicion)
        val txtNombre: TextView = view.findViewById(R.id.nombre)
        val txtPuntuacion: TextView = view.findViewById(R.id.puntuacion)
        val imgFoto: ImageView = view.findViewById(R.id.foto)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UsuarioViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_ranking, parent, false)
        return UsuarioViewHolder(view)
    }

    override fun getItemCount(): Int = usuarios.size

    override fun onBindViewHolder(holder: UsuarioViewHolder, position: Int) {
        val usuario = usuarios[position]

        holder.txtPosicion.text = usuario.posicion.toString()
        holder.txtNombre.text = usuario.nombre
        val rangoTexto = usuario.rango ?: "Cobre"
        holder.txtPuntuacion.text = "${usuario.elo} ELO ($rangoTexto)"

        if (!usuario.fotoUrl.isNullOrEmpty()) {
            Glide.with(context)
                .load(usuario.fotoUrl)
                .diskCacheStrategy(DiskCacheStrategy.NONE)
                .skipMemoryCache(true)
                .placeholder(R.drawable.place_holder)
                .into(holder.imgFoto)
        } else {
            holder.imgFoto.setImageResource(R.drawable.place_holder)
        }

        val typedValue = TypedValue()
        context.theme.resolveAttribute(android.R.attr.colorAccent, typedValue, true)
        val colorAcento = typedValue.data

        holder.itemView.setBackgroundColor(
            if (usuario.esActual) colorAcento else Color.TRANSPARENT
        )

        holder.itemView.setOnClickListener {
            onUserClick(usuario.id)
        }
    }

    fun actualizarLista(nuevaLista: List<UsuarioRankingDB>) {
        this.usuarios = nuevaLista
        notifyDataSetChanged()
    }
}