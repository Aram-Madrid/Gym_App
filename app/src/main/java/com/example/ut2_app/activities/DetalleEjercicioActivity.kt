package com.example.ut2_app.activities

import android.os.Bundle
import android.text.InputType
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.ut2_app.databinding.ActivityDetalleEjercicioBinding
import com.example.ut2_app.model.Ejercicio
import com.example.ut2_app.model.EjercicioDetalle
import com.example.ut2_app.viewmodels.DetalleEjercicioViewModel
import android.util.Log

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

        // 1. Recibir datos del Intent
        idDiaRutina = intent.getStringExtra("ID_DIA_RUTINA")
        idEjercicioExistente = intent.getStringExtra("EJERCICIO_ID")

        // 🔑 Ahora el ID SIEMPRE debe estar presente porque la rutina se crea automáticamente
        if (idDiaRutina == null) {
            Toast.makeText(
                this,
                "Error: Datos incompletos. Vuelve a intentarlo.",
                Toast.LENGTH_LONG
            ).show()
            finish()
            return
        }

        // Configurar título
        title = if (idEjercicioExistente != null) "Editar Ejercicio" else "Añadir Ejercicio"

        observeCatalogo()
        observeViewModelStatus()

        // Botón Establecer Series
        binding.btnEstablecerSeries.setOnClickListener {
            val numSeries = binding.editTextNumSeries.text.toString().toIntOrNull()
            if (numSeries != null && numSeries > 0) {
                mostrarCamposSeries(numSeries)
            } else {
                Toast.makeText(
                    this,
                    "Introduce un número válido de series",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        // Botón Confirmar
        binding.btnConfirmar.setOnClickListener {
            guardarDatos()
        }

        // Cargar datos si estamos en modo Edición
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
                Toast.makeText(
                    this,
                    "No se pudo cargar el catálogo de ejercicios.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun setupSpinner(catalogo: List<EjercicioDetalle>) {
        val nombresEjercicios = catalogo.map { it.nombre }

        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            nombresEjercicios
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerEjercicios.adapter = adapter

        binding.spinnerEjercicios.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                ejercicioSeleccionado = catalogoCompleto[position]
                Log.d("DetalleEjercicio", "Ejercicio seleccionado: ${catalogoCompleto[position].nombre}")
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
            val tv = TextView(this)
            tv.text = "Serie $i"
            tv.textSize = 16f
            tv.setPadding(0, 16, 0, 8)
            binding.linearLayoutSeries.addView(tv)

            val etPeso = EditText(this)
            etPeso.hint = "Peso (kg)"
            etPeso.inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
            binding.linearLayoutSeries.addView(etPeso)

            val etReps = EditText(this)
            etReps.hint = "Repeticiones"
            etReps.inputType = InputType.TYPE_CLASS_NUMBER
            binding.linearLayoutSeries.addView(etReps)

            seriesViews.add(Pair(etPeso, etReps))
        }

        Toast.makeText(
            this,
            "$numSeries series creadas. Completa los datos.",
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun observeViewModelStatus() {
        viewModel.operacionExitosa.observe(this) { message ->
            if (message != null) {
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
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
            Toast.makeText(
                this,
                "Debe seleccionar un ejercicio del catálogo.",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        if (seriesViews.isEmpty()) {
            Toast.makeText(
                this,
                "Debe establecer al menos 1 serie.",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        // Recoger datos de las series
        var totalReps = 0
        var pesoMaximo = 0.0
        var pesoPromedio = 0.0
        var totalPeso = 0.0
        val seriesIncompletas = mutableListOf<Int>()
        var seriesValidas = 0

        seriesViews.forEachIndexed { index, (pesoView, repsView) ->
            val pesoText = pesoView.text.toString().trim()
            val repsText = repsView.text.toString().trim()

            Log.d("DetalleEjercicio", "Serie ${index + 1}: peso='$pesoText', reps='$repsText'")

            if (pesoText.isEmpty() || repsText.isEmpty()) {
                seriesIncompletas.add(index + 1)
            } else {
                val peso = pesoText.toDoubleOrNull()
                val reps = repsText.toIntOrNull()

                if (peso == null || reps == null || peso < 0 || reps < 0) {
                    seriesIncompletas.add(index + 1)
                } else {
                    totalReps += reps
                    totalPeso += peso
                    seriesValidas++

                    if (peso > pesoMaximo) {
                        pesoMaximo = peso
                    }

                    Log.d("DetalleEjercicio", "Serie ${index + 1} válida: peso=$peso, reps=$reps")
                }
            }
        }

        // Validar que todas las series estén completas
        if (seriesIncompletas.isNotEmpty()) {
            Toast.makeText(
                this,
                "Complete las series: ${seriesIncompletas.joinToString(", ")}",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        if (totalReps <= 0) {
            Toast.makeText(
                this,
                "Debe registrar al menos 1 repetición válida.",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        // Calcular peso promedio
        pesoPromedio = if (seriesValidas > 0) totalPeso / seriesValidas else 0.0

        // 🔑 idDiaRutina SIEMPRE es válido aquí (fue verificado en onCreate)
        val idDia = idDiaRutina!!

        Log.d("DetalleEjercicio", "=== RESUMEN PARA GUARDAR ===")
        Log.d("DetalleEjercicio", "Ejercicio: ${selected.nombre}")
        Log.d("DetalleEjercicio", "ID Ejercicio (FK): ${selected.id_ejercicio}")
        Log.d("DetalleEjercicio", "Total Reps: $totalReps")
        Log.d("DetalleEjercicio", "Peso Máximo: $pesoMaximo")
        Log.d("DetalleEjercicio", "Peso Promedio: $pesoPromedio")
        Log.d("DetalleEjercicio", "Series Válidas: $seriesValidas")
        Log.d("DetalleEjercicio", "ID Día: $idDia")

        // Usar peso máximo para el registro (o promedio, según prefieras)
        viewModel.guardarEjercicio(
            idEjercicio = idEjercicioExistente,
            idDiaRutina = idDia,
            nombre = selected.nombre,
            reps = totalReps,
            peso = pesoMaximo, // Puedes cambiar a pesoPromedio si prefieres
            dificultad = 1.0,
            idFkEjercicio = selected.id_ejercicio
        )
    }
}