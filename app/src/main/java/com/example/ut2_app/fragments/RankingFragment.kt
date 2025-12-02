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
import com.example.ut2_app.model.UsuarioRankingDB // Modelo correcto para el Adapter

class RankingFragment : Fragment() {

    private var _binding: FragmentRankingBinding? = null
    // Propiedad calculada para acceder al binding de forma segura
    private val binding get() = _binding!!

    // 1. Inicializar el ViewModel
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

        // Inicializar el Adapter con una lista vacía
        rankingAdapter = RankingAdapter(requireContext(), emptyList())

        binding.recyclerRanking.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = rankingAdapter
        }

        // Observar los datos del ViewModel
        observeRanking()
    }

    override fun onResume() {
        super.onResume()
        // 🔑 NUEVO: Recargar ranking cada vez que el fragment es visible
        viewModel.cargarRanking()
    }

    private fun observeRanking() {
        // Observa la lista de usuarios (UsuarioRankingDB) que viene de Supabase
        viewModel.usuariosRanking.observe(viewLifecycleOwner) { listaUsuarios ->
            // Actualiza la lista en el Adapter
            rankingAdapter.actualizarLista(listaUsuarios)
        }

        // Opcional: Observar otros LiveData del ViewModel (barra de experiencia, etc.)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Limpiar el binding para evitar fugas de memoria
        _binding = null
    }
}