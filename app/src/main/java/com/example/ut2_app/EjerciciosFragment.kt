package com.example.ut2_app

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.ut2_app.adapters.EjercicioAdapter
import com.example.ut2_app.databinding.FragmentEjerciciosBinding
import com.example.ut2_app.model.Ejercicio

class EjerciciosFragment : Fragment() {

    private var _binding: FragmentEjerciciosBinding? = null
    private val binding get() = _binding!!

    private val ejerciciosSeleccionados = mutableListOf<Ejercicio>()
    private val ejerciciosDisponibles = listOf(
        Ejercicio("Flexiones"),
        Ejercicio("Sentadillas"),
        Ejercicio("Abdominales"),
        Ejercicio("Dominadas"),
        Ejercicio("Plancha"),
        Ejercicio("Burpees")
    )

    private lateinit var adapter: EjercicioAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEjerciciosBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = EjercicioAdapter(ejerciciosSeleccionados) { ejercicio ->
            // Abrir DetalleEjercicioFragment pasando el ejercicio
            parentFragmentManager.beginTransaction()
                .replace(
                    R.id.fragment_container,
                    DetalleEjercicioFragment.newInstance(ejercicio)
                )
                .addToBackStack(null)
                .commit()
        }

        binding.recyclerViewEjerciciosSeleccionados.apply {
            layoutManager = LinearLayoutManager(requireContext())
            this.adapter = this@EjerciciosFragment.adapter
        }

        binding.btnAgregarEjercicio.setOnClickListener {
            SeleccionarEjerciciosDialog(requireContext(), ejerciciosDisponibles) { seleccionados ->
                seleccionados.forEach { ejercicio ->
                    if (ejerciciosSeleccionados.none { it.nombre == ejercicio.nombre }) {
                        ejerciciosSeleccionados.add(ejercicio.copy(seleccionado = false))
                    }
                }
                adapter.notifyDataSetChanged()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
