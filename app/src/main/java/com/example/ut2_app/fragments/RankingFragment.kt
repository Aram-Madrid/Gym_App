package com.example.ut2_app.fragments

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.ut2_app.adapters.RankingAdapter
import com.example.ut2_app.adapters.Usuario
import com.example.ut2_app.databinding.FragmentRankingBinding
import com.example.ut2_app.util.SupabaseClientProvider
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.rpc
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

// Definición local de la data class para el ejemplo (mejor en un archivo aparte)
@Serializable
data class UsuarioData(
    val id: String,
    val nombre: String,
    val email: String,
    val elo: Int,
    val fotoperfilurl: String? = null
)

class RankingFragment : Fragment() {

    private var _binding: FragmentRankingBinding? = null
    private val binding get() = _binding!!

    private val supabase = SupabaseClientProvider.supabase

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

    @SuppressLint("NotifyDataSetChanged")
    private fun cargarRankingAmigos() {
        // La lógica de carga funciona correctamente y no se ha modificado.
        val uidActual = supabase.auth.currentUserOrNull()?.id
        if (uidActual == null) {
            Toast.makeText(requireContext(), "Error: usuario no autenticado", Toast.LENGTH_SHORT).show()
            return
        }

        val usuarios = mutableListOf<Usuario>()
        val adapter = RankingAdapter(requireContext(), usuarios)
        binding.recyclerRanking.adapter = adapter

        lifecycleScope.launch {
            try {
                // PASO 1: Obtener los IDs de amigos
                val amigosResponse = supabase.postgrest["amigos"]
                    .select {
                        filter { eq("id", uidActual) }
                    }
                    .decodeList<Map<String, String>>()

                val friendIds = amigosResponse.mapNotNull { it["id_amigo"] }.toMutableList()
                friendIds.add(uidActual)

                if (friendIds.isEmpty()) {
                    adapter.notifyDataSetChanged()
                    return@launch
                }

                // PASO 2: Obtener los datos de perfil
                val usersResponse = supabase.postgrest["usuarios"]
                    .select() {
                        filter { isIn("id", friendIds) }
                    }
                    .decodeList<UsuarioData>()

                // Mapeo al adaptador
                usuarios.clear()
                for (userData in usersResponse) {
                    val esActual = userData.id == uidActual
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

            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Error al cargar ranking: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

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
                // LLAMADA RPC CORREGIDA: Usar el nombre de la función EN MINÚSCULAS
                val result = supabase.postgrest
                    .rpc(
                        function = "get_user_profile_by_email", // ¡TODO EN MINÚSCULAS!
                        parameters = mapOf("p_email" to cleanedEmail)
                    )
                    .decodeList<UsuarioData>()

                if (result.isEmpty()) {
                    // Si la lista está vacía, la función SQL no encontró coincidencias
                    Toast.makeText(requireContext(), "No existe un usuario con ese correo", Toast.LENGTH_SHORT).show()
                    return@launch
                }

                // La función RPC garantiza que la búsqueda por email funcionó.
                val amigo = result.first()
                val amigoId = amigo.id
                val amigoNombre = amigo.nombre

                if (amigoId == userId) {
                    Toast.makeText(requireContext(), "No puedes agregarte a ti mismo", Toast.LENGTH_SHORT).show()
                    return@launch
                }

                // PASO 2: Insertar la relación de amistad
                val amigoData = mapOf(
                    "id" to userId,
                    "id_amigo" to amigoId
                )

                supabase.postgrest["amigos"].insert(listOf(amigoData))

                Toast.makeText(requireContext(), "$amigoNombre añadido a tus amigos", Toast.LENGTH_SHORT).show()
                cargarRankingAmigos()

            } catch (e: Exception) {
                // Esto capturará errores de red o errores de clave duplicada (si ya son amigos)
                Toast.makeText(requireContext(), "Error al agregar amigo: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}