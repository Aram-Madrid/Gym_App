package com.example.ut2_app.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.ut2_app.adapters.RankingAdapter
import com.example.ut2_app.databinding.FragmentRankingBinding
import com.example.ut2_app.viewmodels.RankingViewModel

class RankingFragment : Fragment() {

    private var _binding: FragmentRankingBinding? = null
    // Propiedad calculada para acceder al binding de forma segura
    private val binding get() = _binding!!

    // 1. Inicializar el ViewModel para cargar los datos del ranking
    private val viewModel: RankingViewModel by viewModels()

    private lateinit var rankingAdapter: RankingAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inicializar View Binding
        _binding = FragmentRankingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 2. Inicializar el Adapter con una lista vacía (el ViewModel la llenará)
        rankingAdapter = RankingAdapter(requireContext(), emptyList())

        binding.recyclerRanking.apply { // ⬅️ CORRECCIÓN: Usando el ID real del XML
            layoutManager = LinearLayoutManager(requireContext())
            adapter = rankingAdapter
        }

        // 3. Observar los datos del ViewModel
        observeRanking()

        // 4. (Opcional) Forzar la carga inicial si el ViewModel no la tiene en init{}
        // viewModel.cargarRanking()
    }

    private fun observeRanking() {
        // Observa la lista de usuarios (UsuarioRankingDB)
        viewModel.usuariosRanking.observe(viewLifecycleOwner) { listaUsuarios ->
            // Actualiza la lista en el Adapter
            rankingAdapter.actualizarLista(listaUsuarios)
        }

        // Puedes observar aquí el estado de la barra de experiencia si lo implementas en el ViewModel
        // viewModel.barraExperiencia.observe(viewLifecycleOwner) { porcentaje -> ... }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Limpiar el binding para evitar fugas de memoria
        _binding = null
    }
}