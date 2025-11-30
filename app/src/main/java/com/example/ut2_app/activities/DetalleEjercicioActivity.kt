package com.example.ut2_app.activities

import android.R
import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.ut2_app.databinding.ActivityDetalleEjercicioBinding
import com.example.ut2_app.model.Ejercicio
import com.example.ut2_app.model.Serie

class DetalleEjercicioActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDetalleEjercicioBinding
    private val seriesViews = mutableListOf<Pair<EditText, EditText>>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDetalleEjercicioBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Lista de nombres predefinidos
        val nombresEjercicios = listOf(
            "Flexiones", "Sentadillas", "Abdominales",
            "Dominadas", "Plancha", "Burpees"
        )

        // Configurar el spinner
        val adapter = ArrayAdapter(
            this,
            R.layout.simple_spinner_item,
            nombresEjercicios
        )
        adapter.setDropDownViewResource(R.layout.simple_spinner_dropdown_item)
        binding.spinnerEjercicios.adapter = adapter

        // Botón para generar campos de series
        binding.btnEstablecerSeries.setOnClickListener {
            val numSeries = binding.editTextNumSeries.text.toString().toIntOrNull()
            if (numSeries != null && numSeries > 0) {
                mostrarCamposSeries(numSeries)
            }
        }

        // Botón para confirmar y volver
        binding.btnConfirmar.setOnClickListener {
            val nombreSeleccionado = binding.spinnerEjercicios.selectedItem.toString()
            val ejercicio = Ejercicio(nombreSeleccionado)

            // Guardar series
            seriesViews.forEach { (pesoView, repsView) ->
                val peso = pesoView.text.toString().toFloatOrNull() ?: 0f
                val reps = repsView.text.toString().toIntOrNull() ?: 0
                ejercicio.series.add(Serie(peso, reps))
            }

            // Devolver el ejercicio creado
            val resultIntent = Intent()
            resultIntent.putExtra("nuevo_ejercicio", ejercicio)
            setResult(RESULT_OK, resultIntent)
            finish()
        }
    }

    private fun mostrarCamposSeries(numSeries: Int) {
        binding.linearLayoutSeries.removeAllViews()
        seriesViews.clear()

        for (i in 1..numSeries) {
            val tv = TextView(this)
            tv.text = "Serie $i"
            tv.textSize = 16f
            tv.setPadding(0, 8, 0, 4)
            binding.linearLayoutSeries.addView(tv)

            val etPeso = EditText(this)
            etPeso.hint = "Peso (kg)"
            etPeso.inputType = InputType.TYPE_CLASS_NUMBER or
                    InputType.TYPE_NUMBER_FLAG_DECIMAL
            binding.linearLayoutSeries.addView(etPeso)

            val etReps = EditText(this)
            etReps.hint = "Repeticiones"
            etReps.inputType = InputType.TYPE_CLASS_NUMBER
            binding.linearLayoutSeries.addView(etReps)

            seriesViews.add(Pair(etPeso, etReps))
        }
    }
}
