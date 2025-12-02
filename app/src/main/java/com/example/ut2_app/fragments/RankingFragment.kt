package com.example.ut2_app.fragments

import android.app.AlertDialog
import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.ut2_app.R
import com.example.ut2_app.adapters.RankingAdapter
import com.example.ut2_app.databinding.FragmentRankingBinding
import com.example.ut2_app.viewmodels.RankingViewModel
import com.example.ut2_app.util.AuthManager
import com.example.ut2_app.util.SupabaseClientProvider
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.rpc
import kotlinx.coroutines.launch

class RankingFragment : Fragment() {

    private var _binding: FragmentRankingBinding? = null
    private val binding get() = _binding!!

    private val viewModel: RankingViewModel by viewModels()
    private lateinit var rankingAdapter: RankingAdapter


    private val eloUmbrales = mapOf(
        "Cobre" to 0, "Bronze" to 500, "Plata" to 1000, "Oro" to 1500,
        "Esmeralda" to 2000, "Diamante" to 2500, "Campeon" to 3000
    )

    private fun calcularProgresoHaciaSiguienteRango(eloActual: Short): Int {
        val umbralesValores = eloUmbrales.values.toList().sorted()
        val minEloActual = umbralesValores.lastOrNull { it <= eloActual } ?: 0
        val minEloSiguiente = umbralesValores.firstOrNull { it > eloActual }

        if (minEloSiguiente == null || minEloSiguiente == minEloActual) {
            return if (eloActual >= 3000) 100 else 0
        }

        val eloDiferenciaTotal = minEloSiguiente - minEloActual
        val eloGanadoEnRango = eloActual - minEloActual

        if (eloDiferenciaTotal <= 0) return 0

        val progreso = (eloGanadoEnRango.toDouble() / eloDiferenciaTotal.toDouble()) * 100
        return progreso.toInt().coerceIn(0, 100)
    }

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



    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRankingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Callback de navegación
        rankingAdapter = RankingAdapter(requireContext(), emptyList()) { userId ->
            abrirPerfilUsuario(userId)
        }

        binding.recyclerRanking.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = rankingAdapter
        }

        binding.fabAddFriend.setOnClickListener {
            mostrarDialogoAgregarAmigo()
        }

        observeRanking()
    }

    // ... (onResume y abrirPerfilUsuario sin cambios) ...

    override fun onResume() {
        super.onResume()
        viewModel.cargarRanking()
    }

    private fun observeRanking() {
        viewModel.usuariosRanking.observe(viewLifecycleOwner) { listaUsuarios ->
            rankingAdapter.actualizarLista(listaUsuarios)

            // --- LÓGICA DE ASIGNACIÓN DE EMBLEMA Y BARRA DE PROGRESO (RESTAURADA) ---
            val usuarioActual = listaUsuarios.find { it.esActual }

            if (usuarioActual != null) {
                // 1. Asignar Emblema
                val rangoActual = usuarioActual.rango
                if (!rangoActual.isNullOrEmpty()) {
                    val emblemaId = getEmblemaResourceId(rangoActual)
                    binding.emblemaRanking.setImageResource(emblemaId)
                } else {
                    binding.emblemaRanking.setImageResource(R.drawable.place_holder)
                }

                // 2. Asignar Progreso de Barra
                val progreso = calcularProgresoHaciaSiguienteRango(usuarioActual.elo)
                binding.barraExperiencia.progress = progreso

            } else {
                binding.emblemaRanking.setImageResource(R.drawable.place_holder)
                binding.barraExperiencia.progress = 0
            }
            // --------------------------------------------------------------------------
        }
    }

    private fun abrirPerfilUsuario(userId: String) {
        // Preparamos los datos
        val bundle = Bundle().apply {
            putString("USER_ID", userId)
        }

        try {
            findNavController().navigate(R.id.action_rankingFragment_to_userProfileFragment, bundle)
        } catch (e: Exception) {
            try {
                findNavController().navigate(R.id.userProfileFragment, bundle)
            } catch (e2: Exception) {
                Toast.makeText(requireContext(), "Error de navegación: ${e2.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }


    // ... (Lógica de Añadir Amigo sin cambios) ...

    private fun mostrarDialogoAgregarAmigo() {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Añadir Amigo")
        builder.setMessage("Introduce el correo de tu amigo:")

        val input = EditText(requireContext())
        input.inputType = InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS

        val container = android.widget.FrameLayout(requireContext())
        val params = android.widget.FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        val margin = resources.getDimensionPixelSize(com.example.ut2_app.R.dimen.margin_medium)
        params.leftMargin = margin
        params.rightMargin = margin
        input.layoutParams = params
        container.addView(input)

        builder.setView(container)

        builder.setPositiveButton("Añadir") { _, _ ->
            val emailAmigo = input.text.toString().trim()
            if (emailAmigo.isNotEmpty()) {
                agregarAmigo(emailAmigo)
            }
        }
        builder.setNegativeButton("Cancelar") { dialog, _ -> dialog.cancel() }

        builder.show()
    }

    private fun agregarAmigo(emailAmigo: String) {
        lifecycleScope.launch {
            try {
                val userId = AuthManager.getCurrentUserId() ?: return@launch

                val respuesta = SupabaseClientProvider.supabase.postgrest.rpc(
                    "agregar_amigo_por_email",
                    mapOf(
                        "p_id_usuario" to userId,
                        "p_email_amigo" to emailAmigo
                    )
                ).decodeAs<String>()

                Toast.makeText(requireContext(), respuesta, Toast.LENGTH_LONG).show()
                viewModel.cargarRanking()

            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}