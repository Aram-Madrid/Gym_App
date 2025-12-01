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
// *************** IMPORTAR LA ESTRATEGIA DE CACHÉ ***************
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.example.ut2_app.R
import com.example.ut2_app.model.UsuarioRankingDB // ⬅️ Nuevo Modelo

class RankingAdapter(
    private val context: Context,
    // 🔑 Aceptar la lista del nuevo modelo
    private var usuarios: List<UsuarioRankingDB>
) : RecyclerView.Adapter<RankingAdapter.UsuarioViewHolder>() {

    // ... (UsuarioViewHolder se mantiene igual, ya que usa los IDs del layout)

    class UsuarioViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val txtPosicion: TextView = view.findViewById(R.id.posicion)
        val txtNombre: TextView = view.findViewById(R.id.nombre)
        val txtPuntuacion: TextView = view.findViewById(R.id.puntuacion)
        val imgFoto: ImageView = view.findViewById(R.id.foto)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UsuarioViewHolder {
        // Asegúrate de que R.layout.item_ranking existe
        val view = LayoutInflater.from(context).inflate(R.layout.item_ranking, parent, false)
        return UsuarioViewHolder(view)
    }

    override fun getItemCount(): Int = usuarios.size

    override fun onBindViewHolder(holder: UsuarioViewHolder, position: Int) {
        val usuario = usuarios[position]

        // 🔑 Los campos ahora vienen del modelo UsuarioRankingDB
        holder.txtPosicion.text = usuario.posicion.toString()
        holder.txtNombre.text = usuario.nombre
        // Usamos elo que es Short (Integer)
        holder.txtPuntuacion.text = "${usuario.elo} ELO"

        // Carga la foto (usando fotoUrl que mapea fotoperfilurl)
        if (!usuario.fotoUrl.isNullOrEmpty()) {
            Glide.with(context)
                .load(usuario.fotoUrl)
                // *************** LÍNEAS AGREGADAS PARA SOLUCIONAR EL CACHÉ ***************
                .diskCacheStrategy(DiskCacheStrategy.NONE) // Fuerza a ignorar la caché de disco
                .skipMemoryCache(true)                      // Fuerza a ignorar la caché de memoria
                // *************************************************************************
                .placeholder(R.drawable.place_holder)
                .into(holder.imgFoto)
        } else {
            holder.imgFoto.setImageResource(R.drawable.place_holder)
        }

        // Resaltar usuario actual (usa la propiedad 'esActual' ya asignada en el ViewModel)
        val typedValue = TypedValue()
        context.theme.resolveAttribute(android.R.attr.colorAccent, typedValue, true)
        val colorAcento = typedValue.data

        holder.itemView.setBackgroundColor(
            if (usuario.esActual) colorAcento
            else Color.TRANSPARENT
        )
    }

    // 🔑 Método para actualizar la lista desde el ViewModel
    fun actualizarLista(nuevaLista: List<UsuarioRankingDB>) {
        this.usuarios = nuevaLista
        notifyDataSetChanged()
    }
}