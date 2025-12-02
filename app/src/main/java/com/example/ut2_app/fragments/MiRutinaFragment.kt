package com.example.ut2_app.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.ut2_app.adapters.RutinaAdapter
import com.example.ut2_app.databinding.FragmentMiRutinaBinding
import com.example.ut2_app.util.VerticalSpaceItemDecoration
import com.example.ut2_app.viewmodels.MiRutinaViewModel

class MiRutinaFragment : Fragment() {

    private var _binding: FragmentMiRutinaBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MiRutinaViewModel by viewModels()
    private lateinit var rutinaAdapter: RutinaAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMiRutinaBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        rutinaAdapter = RutinaAdapter(
            listaItems = emptyList(),
            lifecycleOwner = viewLifecycleOwner,
            onCrearRutina = { nombreDia, fechaObjetivo ->
                viewModel.crearRutinaDia(nombreDia, fechaObjetivo)
            }
        )

        binding.recyclerViewRutinas.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = rutinaAdapter
            addItemDecoration(VerticalSpaceItemDecoration(20))
        }

        observeDiasRutina()
        observeErrors()
    }

    private fun observeDiasRutina() {
        viewModel.itemsRutina.observe(viewLifecycleOwner) { listaVisual ->
            rutinaAdapter.actualizarLista(listaVisual)
        }

        viewModel.isLoading.observe(viewLifecycleOwner) {
            // ProgressBar logic
        }
    }

    private fun observeErrors() {
        viewModel.error.observe(viewLifecycleOwner) { error ->
            if (error != null) {
                Toast.makeText(requireContext(), error, Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.cargarRutinas()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}