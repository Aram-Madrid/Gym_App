package com.example.ut2_app

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.TextView
import com.bumptech.glide.Glide

data class Usuario(
    val nombre: String,
    val puntuacion: Int,
    val fotoUrl: String?,
    var posicion: Int,
    val esActual: Boolean = false
)

class RankingAdapter(private val context: Context, private val usuarios: List<Usuario>) : BaseAdapter() {

    override fun getCount(): Int = usuarios.size

    override fun getItem(position: Int): Any = usuarios[position]

    override fun getItemId(position: Int): Long = position.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val view = convertView ?: LayoutInflater.from(context).inflate(R.layout.item_ranking, parent, false)

        val usuario = usuarios[position]
        val txtPosicion = view.findViewById<TextView>(R.id.posicion)
        val txtNombre = view.findViewById<TextView>(R.id.nombre)
        val txtPuntuacion = view.findViewById<TextView>(R.id.puntuacion)
        val imgFoto = view.findViewById<ImageView>(R.id.foto)

        txtPosicion.text = usuario.posicion.toString()
        txtNombre.text = usuario.nombre
        txtPuntuacion.text = "${usuario.puntuacion} pts"

        // Carga la imagen (si existe) o un placeholder
        if (!usuario.fotoUrl.isNullOrEmpty()) {
            Glide.with(context)
                .load(usuario.fotoUrl)
                .placeholder(R.drawable.place_holder)
                .into(imgFoto)
        } else {
            imgFoto.setImageResource(R.drawable.place_holder)
        }

        if (usuario.esActual){
            view.setBackgroundColor(context.getColor(R.color.hint_color))
        } else {
            view.setBackgroundColor(context.getColor(android.R.color.transparent))
        }

        return view
    }
}
