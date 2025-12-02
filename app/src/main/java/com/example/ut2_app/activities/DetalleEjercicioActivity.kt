package com.example.ut2_app.activities

import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.ut2_app.databinding.ActivityDetalleEjercicioBinding
import com.example.ut2_app.model.EjercicioDetalle
import com.example.ut2_app.viewmodels.DetalleEjercicioViewModel

class DetalleEjercicioActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDetalleEjercicioBinding
    private val seriesViews = mutableListOf<Pair<EditText, EditText>>()

    private val viewModel: DetalleEjercicioViewModel by viewModels()
    private var ejercicioSeleccionado: EjercicioDetalle? = null
    private var catalogoCompleto: List<EjercicioDetalle> = emptyList()

    private var idDiaRutina: String? = null
    private var idEjercicioExistente: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDetalleEjercicioBinding.inflate(layoutInflater)
        setContentView(binding.root)

        idDiaRutina = intent.getStringExtra("ID_DIA_RUTINA")
        idEjercicioExistente = intent.getStringExtra("EJERCICIO_ID")

        if (idDiaRutina == null) {
            Toast.makeText(this, "Error: Datos incompletos.", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        title = if (idEjercicioExistente != null) "Editar Ejercicio" else "Añadir Ejercicio"

        observeCatalogo()
        observeViewModelStatus()

        binding.btnEstablecerSeries.setOnClickListener {
            val numSeries = binding.editTextNumSeries.text.toString().toIntOrNull()
            if (numSeries != null && numSeries > 0) {
                mostrarCamposSeries(numSeries)
            } else {
                Toast.makeText(this, "Introduce un número válido de series", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnConfirmar.setOnClickListener {
            guardarDatos()
        }

        if (idEjercicioExistente != null) {
            viewModel.cargarEjercicio(idEjercicioExistente!!)
        }
    }

    private fun observeCatalogo() {
        viewModel.isLoading.observe(this) { isLoading ->
            binding.spinnerEjercicios.isEnabled = !isLoading
            binding.btnConfirmar.isEnabled = !isLoading
            binding.btnEstablecerSeries.isEnabled = !isLoading
        }

        viewModel.catalogoEjercicios.observe(this) { catalogo ->
            if (catalogo.isNotEmpty()) {
                catalogoCompleto = catalogo
                setupSpinner(catalogo)
            } else if (viewModel.isLoading.value == false) {
                Toast.makeText(this, "No se pudo cargar el catálogo.", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun setupSpinner(catalogo: List<EjercicioDetalle>) {
        val nombresEjercicios = catalogo.map { ejercicio ->
            val grupo = ejercicio.grupo_muscular ?: "Sin grupo"
            "${ejercicio.nombre} ($grupo)"
        }

        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, nombresEjercicios)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerEjercicios.adapter = adapter

        binding.spinnerEjercicios.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                ejercicioSeleccionado = catalogoCompleto[position]
                Log.d("DetalleEjercicio", "Seleccionado: ${ejercicioSeleccionado?.nombre}")
                Log.d("DetalleEjercicio", "  Grupo: ${ejercicioSeleccionado?.grupo_muscular}")
                Log.d("DetalleEjercicio", "  Dificultad: ${ejercicioSeleccionado?.dificultad}")
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {
                ejercicioSeleccionado = null
            }
        }
    }

    private fun mostrarCamposSeries(numSeries: Int) {
        binding.linearLayoutSeries.removeAllViews()
        seriesViews.clear()

        for (i in 1..numSeries) {
            val tv = TextView(this).apply {
                text = "Serie $i"
                textSize = 16f
                setPadding(0, 16, 0, 8)
            }
            binding.linearLayoutSeries.addView(tv)

            val etPeso = EditText(this).apply {
                hint = "Peso (kg)"
                inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
            }
            binding.linearLayoutSeries.addView(etPeso)

            val etReps = EditText(this).apply {
                hint = "Repeticiones"
                inputType = InputType.TYPE_CLASS_NUMBER
            }
            binding.linearLayoutSeries.addView(etReps)

            seriesViews.add(Pair(etPeso, etReps))
        }

        Toast.makeText(this, "$numSeries series creadas.", Toast.LENGTH_SHORT).show()
    }

    private fun observeViewModelStatus() {
        viewModel.operacionExitosa.observe(this) { message ->
            if (message != null) {
                Toast.makeText(this, message, Toast.LENGTH_LONG).show()
                viewModel.clearOperationStatus()
                setResult(RESULT_OK)
                finish()
            }
        }

        viewModel.error.observe(this) { error ->
            if (error != null) {
                Toast.makeText(this, "Error: $error", Toast.LENGTH_LONG).show()
                viewModel.clearOperationStatus()
            }
        }
    }

    private fun guardarDatos() {
        val selected = ejercicioSeleccionado

        if (selected == null) {
            Toast.makeText(this, "Seleccione un ejercicio.", Toast.LENGTH_SHORT).show()
            return
        }

        if (seriesViews.isEmpty()) {
            Toast.makeText(this, "Establezca al menos 1 serie.", Toast.LENGTH_SHORT).show()
            return
        }

        // Validar y recoger series
        val seriesParaGuardar = mutableListOf<Pair<Double, Int>>()
        val seriesIncompletas = mutableListOf<Int>()

        seriesViews.forEachIndexed { index, (pesoView, repsView) ->
            val pesoText = pesoView.text.toString().trim()
            val repsText = repsView.text.toString().trim()

            if (pesoText.isEmpty() || repsText.isEmpty()) {
                seriesIncompletas.add(index + 1)
            } else {
                val peso = pesoText.toDoubleOrNull()
                val reps = repsText.toIntOrNull()

                if (peso == null || reps == null || peso < 0 || reps <= 0) {
                    seriesIncompletas.add(index + 1)
                } else {
                    seriesParaGuardar.add(Pair(peso, reps))
                }
            }
        }

        if (seriesIncompletas.isNotEmpty()) {
            Toast.makeText(this, "Complete las series: ${seriesIncompletas.joinToString(", ")}", Toast.LENGTH_LONG).show()
            return
        }

        if (seriesParaGuardar.isEmpty()) {
            Toast.makeText(this, "Registre al menos 1 serie válida.", Toast.LENGTH_SHORT).show()
            return
        }

        // Obtener datos del catálogo
        val dificultadDelCatalogo = selected.dificultad ?: 0.5
        val grupoMuscularDelCatalogo = selected.grupo_muscular ?: "Otro"

        Log.d("DetalleEjercicio", "════════════════════════════════════════")
        Log.d("DetalleEjercicio", "ENVIANDO A GUARDAR:")
        Log.d("DetalleEjercicio", "  Ejercicio: ${selected.nombre}")
        Log.d("DetalleEjercicio", "  Grupo: $grupoMuscularDelCatalogo")
        Log.d("DetalleEjercicio", "  Dificultad: $dificultadDelCatalogo")
        Log.d("DetalleEjercicio", "  Series: ${seriesParaGuardar.size}")
        seriesParaGuardar.forEachIndexed { i, (peso, reps) ->
            Log.d("DetalleEjercicio", "    Serie ${i+1}: ${peso}kg × $reps reps")
        }
        Log.d("DetalleEjercicio", "════════════════════════════════════════")

        viewModel.guardarEjercicioConSeries(
            idEjercicio = idEjercicioExistente,
            idDiaRutina = idDiaRutina!!,
            nombre = selected.nombre,
            seriesData = seriesParaGuardar,
            dificultad = dificultadDelCatalogo,
            idFkEjercicio = selected.id_ejercicio,
            grupoMuscular = grupoMuscularDelCatalogo
        )
    }
}