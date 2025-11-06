package com.example.ut2_app.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.ut2_app.adapters.RutinaAdapter
import com.example.ut2_app.databinding.FragmentMiRutinaBinding
import com.example.ut2_app.model.Rutina
import com.example.ut2_app.util.VerticalSpaceItemDecoration

class MiRutinaFragment : Fragment() {

    private var _binding: FragmentMiRutinaBinding? = null
    private val binding get() = _binding!!

    private val diasDeLaSemana = listOf(
        Rutina("Lunes"),
        Rutina("Martes"),
        Rutina("Miércoles"),
        Rutina("Jueves"),
        Rutina("Viernes"),
        Rutina("Sábado"),
        Rutina("Domingo")
    )

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMiRutinaBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.recyclerViewRutinas.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = RutinaAdapter(diasDeLaSemana)
            addItemDecoration(VerticalSpaceItemDecoration(30))
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
