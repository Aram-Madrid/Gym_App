package com.example.ut2_app

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.ut2_app.databinding.FragmentDetalleEjercicioBinding
import com.example.ut2_app.model.Ejercicio
import com.example.ut2_app.model.Serie

class DetalleEjercicioFragment : Fragment() {

    private var _binding: FragmentDetalleEjercicioBinding? = null
    private val binding get() = _binding!!

    private lateinit var ejercicio: Ejercicio
    private val seriesViews = mutableListOf<Pair<EditText, EditText>>()

    companion object {
        private const val ARG_EJERCICIO = "arg_ejercicio"

        fun newInstance(ejercicio: Ejercicio): DetalleEjercicioFragment {
            val fragment = DetalleEjercicioFragment()
            val bundle = Bundle()
            bundle.putSerializable(ARG_EJERCICIO, ejercicio)
            fragment.arguments = bundle
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ejercicio = arguments?.getSerializable(ARG_EJERCICIO) as Ejercicio
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDetalleEjercicioBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.textViewNombreEjercicio.text = ejercicio.nombre

        binding.btnEstablecerSeries.setOnClickListener {
            val numSeriesText = binding.editTextNumSeries.text.toString()
            val numSeries = numSeriesText.toIntOrNull()
            if (numSeries != null && numSeries > 0) {
                mostrarCamposSeries(numSeries)
            }
        }

        binding.btnGuardarSeries.setOnClickListener {
            guardarSeries()
            parentFragmentManager.popBackStack()
        }
    }

    private fun mostrarCamposSeries(numSeries: Int) {
        binding.linearLayoutSeries.removeAllViews()
        seriesViews.clear()


        for (i in 1..numSeries) {
            val tv = TextView(requireContext())
            tv.text = "Serie $i"
            tv.textSize = 16f
            tv.setPadding(0, 8, 0, 4)
            binding.linearLayoutSeries.addView(tv)

            val etPeso = EditText(requireContext())
            etPeso.hint = "Peso (kg)"
            etPeso.inputType = android.text.InputType.TYPE_CLASS_NUMBER or
                    android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
            binding.linearLayoutSeries.addView(etPeso)

            val etReps = EditText(requireContext())
            etReps.hint = "Repeticiones"
            etReps.inputType = android.text.InputType.TYPE_CLASS_NUMBER
            binding.linearLayoutSeries.addView(etReps)

            seriesViews.add(Pair(etPeso, etReps))
        }
    }

    private fun guardarSeries() {
        ejercicio.series.clear()
        seriesViews.forEach { (pesoView, repsView) ->
            val peso = pesoView.text.toString().toFloatOrNull() ?: 0f
            val reps = repsView.text.toString().toIntOrNull() ?: 0
            ejercicio.series.add(Serie(peso, reps))
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
