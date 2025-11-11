package com.example.ut2_app

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

data class Usuario(
    val nombre: String,
    val puntuacion: Int,
    val fotoUrl: String? = null,
    var posicion: Int = 0,
    val esActual: Boolean = false
)

class RankingAdapter(
    private val context: Context,
    private val usuarios: List<Usuario>
) : RecyclerView.Adapter<RankingAdapter.UsuarioViewHolder>() {

    // ViewHolder
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
        holder.txtPuntuacion.text = "${usuario.puntuacion} pts"

        // Carga la foto
        if (!usuario.fotoUrl.isNullOrEmpty()) {
            Glide.with(context)
                .load(usuario.fotoUrl)
                .placeholder(R.drawable.place_holder)
                .into(holder.imgFoto)
        } else {
            holder.imgFoto.setImageResource(R.drawable.place_holder)
        }

        // Resaltar usuario actual
        val typedValue = TypedValue()
        context.theme.resolveAttribute(android.R.attr.colorAccent, typedValue, true)
        val colorAcento = typedValue.data

        holder.itemView.setBackgroundColor(
            if (usuario.esActual) colorAcento
            else Color.TRANSPARENT
        )

    }
}
