package com.example.ut2_app.fragments

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.ut2_app.R
import com.example.ut2_app.adapters.RankingAdapter
import com.example.ut2_app.adapters.Usuario
import com.example.ut2_app.databinding.FragmentRankingBinding
import com.example.ut2_app.util.SupabaseClientProvider
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.rpc
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

// CLASE DATA: DEBE incluir el campo 'rango'
@Serializable
data class UsuarioData(
    val id: String,
    val nombre: String,
    val email: String,
    val elo: Int,
    val fotoperfilurl: String? = null,
    val rango: String? = null
)

class RankingFragment : Fragment() {

    private var _binding: FragmentRankingBinding? = null
    private val binding get() = _binding!!

    private val supabase = SupabaseClientProvider.supabase

    /**
     * Define los umbrales mínimos de ELO para cada rango.
     * * CORRECCIÓN: Se quita el .toSortedMap(compareBy { eloUmbrales[it] })
     * para evitar el error de recursividad en el compilador.
     * Se usa un mapa simple, y la función de cálculo usará los valores ordenados.
     */
    private val eloUmbrales = mapOf(
        "Cobre" to 0,
        "Bronze" to 500,
        "Plata" to 1000,
        "Oro" to 1500,
        "Esmeralda" to 2000,
        "Diamante" to 2500,
        "Campeon" to 3000
    )

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRankingBinding.inflate(inflater, container, false)

        binding.btnAgregarAmigo.setOnClickListener {
            mostrarDialogoAgregarAmigo()
        }

        binding.recyclerRanking.layoutManager = LinearLayoutManager(requireContext())

        cargarRankingAmigos()

        return binding.root
    }

    // --- LÓGICA DE EMBLEMAS Y PROGRESO DE RANGO ---

    /**
     * Calcula el porcentaje de progreso (0-100) hacia el siguiente rango.
     */
    private fun calcularProgresoHaciaSiguienteRango(eloActual: Int): Int {
        // Obtenemos los valores de ELO y los ordenamos para el cálculo
        val umbralesValores = eloUmbrales.values.toList().sorted()

        // Encontrar el umbral de la liga actual (minEloActual)
        val minEloActual = umbralesValores.lastOrNull { it <= eloActual } ?: 0

        // Encontrar el umbral de la siguiente liga (minEloSiguiente)
        val minEloSiguiente = umbralesValores.firstOrNull { it > eloActual }

        if (minEloSiguiente == null || minEloSiguiente == minEloActual) {
            // Estás en el rango más alto ("Campeon") o no hay siguiente rango.
            return if (eloActual >= 3000) 100 else 0
        }

        val eloDiferenciaTotal = minEloSiguiente - minEloActual
        val eloGanadoEnRango = eloActual - minEloActual

        if (eloDiferenciaTotal <= 0) return 0

        // Fórmula: (Progreso Actual / Diferencia Total) * 100
        val progreso = (eloGanadoEnRango.toDouble() / eloDiferenciaTotal.toDouble()) * 100

        return progreso.toInt().coerceIn(0, 100)
    }

    /**
     * Mapea el nombre del rango (obtenido de la BD) al ID de recurso Drawable.
     */
    private fun getEmblemaResourceId(nombreRango: String): Int {
        return when (nombreRango) {
            "Campeon" -> R.drawable.ic_rank_campeon
            "Diamante" -> R.drawable.ic_rank_diamante
            "Esmeralda" -> R.drawable.ic_rank_esmeralda
            "Oro" -> R.drawable.ic_rank_oro
            "Plata" -> R.drawable.ic_rank_plata
            "Bronze" -> R.drawable.ic_rank_bronce
            "Cobre" -> R.drawable.ic_rank_cobre
            else -> R.drawable.place_holder
        }
    }

    // --- LÓGICA DE CARGA DE RANKING ---

    @SuppressLint("NotifyDataSetChanged")
    private fun cargarRankingAmigos() {
        val uidActual = supabase.auth.currentUserOrNull()?.id
        if (uidActual == null) {
            Toast.makeText(requireContext(), "Error: usuario no autenticado", Toast.LENGTH_SHORT).show()
            return
        }

        val usuarios = mutableListOf<Usuario>()
        val adapter = RankingAdapter(requireContext(), usuarios)
        binding.recyclerRanking.adapter = adapter

        binding.emblemaRanking.setImageResource(R.drawable.place_holder)

        lifecycleScope.launch {
            try {
                // PASO 1: Obtener los IDs de amigos
                val amigosResponse = supabase.postgrest["amigos"]
                    .select { filter { eq("id", uidActual) } }
                    .decodeList<Map<String, String>>()

                val friendIds = amigosResponse.mapNotNull { it["id_amigo"] }.toMutableList()
                friendIds.add(uidActual) // Añadir el ID del usuario actual

                if (friendIds.isEmpty()) {
                    adapter.notifyDataSetChanged()
                    return@launch
                }

                // PASO 2: Obtener los datos de perfil, incluido el campo 'rango'
                val usersResponse = supabase.postgrest["usuarios"]
                    .select() { filter { isIn("id", friendIds) } }
                    .decodeList<UsuarioData>()

                var rangoUsuarioActual: String? = null
                var eloUsuarioActual: Int? = null

                // Mapeo al adaptador y captura del RANGO y ELO del usuario actual
                usuarios.clear()
                for (userData in usersResponse) {
                    val esActual = userData.id == uidActual
                    if (esActual) {
                        rangoUsuarioActual = userData.rango
                        eloUsuarioActual = userData.elo
                    }
                    usuarios.add(
                        Usuario(
                            nombre = userData.nombre,
                            puntuacion = userData.elo,
                            fotoUrl = userData.fotoperfilurl,
                            posicion = 0,
                            esActual = esActual
                        )
                    )
                }

                // Lógica de clasificación
                usuarios.sortByDescending { it.puntuacion }
                usuarios.forEachIndexed { index, usuario -> usuario.posicion = index + 1 }

                adapter.notifyDataSetChanged()

                // --- ASIGNACIÓN DE EMBLEMA USANDO EL CAMPO 'rango' DE LA BD ---
                if (rangoUsuarioActual != null) {
                    val emblemaId = getEmblemaResourceId(rangoUsuarioActual)
                    binding.emblemaRanking.setImageResource(emblemaId)
                    Log.d("RANKING_FRAG", "Rango obtenido de BD: $rangoUsuarioActual. Emblema asignado: $emblemaId")
                } else {
                    Log.w("RANKING_FRAG", "El campo 'rango' es null para el usuario actual.")
                    binding.emblemaRanking.setImageResource(R.drawable.place_holder)
                }

                // --- ASIGNACIÓN DEL PROGRESO DE LA BARRA ---
                if (eloUsuarioActual != null) {
                    val progreso = calcularProgresoHaciaSiguienteRango(eloUsuarioActual)
                    binding.barraExperiencia.progress = progreso
                    Log.d("RANKING_FRAG", "Progreso de liga calculado: $progreso%")
                } else {
                    binding.barraExperiencia.progress = 0
                }
                // -----------------------------------------------------

            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Error al cargar ranking: ${e.message}", Toast.LENGTH_LONG).show()
                Log.e("RANKING_FRAG", "Error en cargarRankingAmigos: ", e)
            }
        }
    }

    // --- LÓGICA DE AGREGAR AMIGO (sin cambios) ---

    private fun mostrarDialogoAgregarAmigo() {
        val context = requireContext()
        val builder = AlertDialog.Builder(context)
        builder.setTitle("Agregar amigo")

        val input = EditText(context)
        input.hint = "Correo del amigo"
        builder.setView(input)

        builder.setPositiveButton("Agregar") { dialog, _ ->
            val texto = input.text.toString().trim()
            if (texto.isNotEmpty()) {
                agregarAmigo(texto)
            } else {
                Toast.makeText(context, "Introduce un correo", Toast.LENGTH_SHORT).show()
            }
            dialog.dismiss()
        }

        builder.setNegativeButton("Cancelar") { dialog, _ -> dialog.dismiss() }
        builder.show()
    }


    private fun agregarAmigo(emailAmigo: String) {
        val userId = supabase.auth.currentUserOrNull()?.id?: return

        val cleanedEmail = emailAmigo.trim()

        lifecycleScope.launch {
            try {
                // LLAMADA RPC para obtener el ID del amigo por email (sin cambios)
                val result = supabase.postgrest
                    .rpc(
                        function = "get_user_profile_by_email",
                        parameters = mapOf("p_email" to cleanedEmail)
                    )
                    .decodeList<UsuarioData>()

                if (result.isEmpty()) {
                    Toast.makeText(requireContext(), "No existe un usuario con ese correo", Toast.LENGTH_SHORT).show()
                    return@launch
                }

                val amigo = result.first()
                val amigoId = amigo.id
                val amigoNombre = amigo.nombre

                if (amigoId == userId) {
                    Toast.makeText(requireContext(), "No puedes agregarte a ti mismo", Toast.LENGTH_SHORT).show()
                    return@launch
                }

                // PASO 2: Insertar la relación de amistad (sin cambios)
                val amigoData = mapOf(
                    "id" to userId,
                    "id_amigo" to amigoId
                )

                supabase.postgrest["amigos"].insert(listOf(amigoData))

                Toast.makeText(requireContext(), "$amigoNombre añadido a tus amigos", Toast.LENGTH_SHORT).show()
                cargarRankingAmigos()

            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Error al agregar amigo: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}