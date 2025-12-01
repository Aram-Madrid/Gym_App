package com.example.ut2_app.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.ut2_app.adapters.RutinaAdapter
import com.example.ut2_app.databinding.FragmentMiRutinaBinding
import com.example.ut2_app.model.DiaSemanaUI // Modelo que usa el Adapter
import com.example.ut2_app.util.VerticalSpaceItemDecoration
import com.example.ut2_app.viewmodels.MiRutinaViewModel

class MiRutinaFragment : Fragment() {

    private var _binding: FragmentMiRutinaBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MiRutinaViewModel by viewModels()

    // 🔑 CORRECCIÓN: Declaración sin argumentos de tipo (sin <...>)
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

        // Inicialización: Pasamos una lista vacía. El Adapter ahora debe aceptar List<DiaSemanaUI>
        rutinaAdapter = RutinaAdapter(emptyList())

        binding.recyclerViewRutinas.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = rutinaAdapter
            addItemDecoration(VerticalSpaceItemDecoration(30))
        }

        observeDiasRutina()
    }

    // 🔑 FUNCIÓN CORREGIDA: Observamos el LiveData 'diasSemana' del ViewModel
    private fun observeDiasRutina() {
        // Observamos el LiveData que devuelve la lista combinada (Lunes-Domingo)
        viewModel.diasSemana.observe(viewLifecycleOwner) { listaDiasCombinada ->
            // El Adapter recibe la lista de DiaSemanaUI
            rutinaAdapter.actualizarLista(listaDiasCombinada)
        }
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}