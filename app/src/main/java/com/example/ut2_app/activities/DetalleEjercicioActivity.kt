package com.example.ut2_app.activities

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.ut2_app.R
import com.example.ut2_app.databinding.ActivityDetalleEjercicioBinding
import com.example.ut2_app.model.Ejercicio
import com.example.ut2_app.viewmodels.DetalleEjercicioViewModel
import android.util.Log

class DetalleEjercicioActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDetalleEjercicioBinding
    private val viewModel: DetalleEjercicioViewModel by viewModels()

    // Datos recibidos del Intent
    private var idDiaRutina: String? = null // ID del día al que se asocia
    private var idEjercicioExistente: String? = null // ID si estamos editando

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDetalleEjercicioBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 1. Recibir datos del Intent
        idDiaRutina = intent.getStringExtra("ID_DIA_RUTINA")
        idEjercicioExistente = intent.getStringExtra("EJERCICIO_ID")

        if (idDiaRutina.isNullOrBlank()) {
            Toast.makeText(this, "Error: Faltan datos esenciales de la rutina.", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        // ❌ No se llama a setSupportActionBar para evitar el error de referencia
        // pero la Activity tiene un título por defecto

        observeViewModel()

        // 2. Cargar datos si estamos en modo Edición
        if (idEjercicioExistente != null) {
            viewModel.cargarEjercicio(idEjercicioExistente!!)
            title = "Cargando..."
        } else {
            title = "Añadir Ejercicio"
        }

        // Mapear el botón Confirmar
        binding.btnConfirmar.setOnClickListener {
            guardarDatos()
        }
    }

    // Método para llenar los campos de la UI (Modo Edición)
    private fun llenarCampos(ejercicio: Ejercicio) {
        // 🔑 ASUMIMOS que 'etNombre' y el resto de IDs ya existen en el XML:
        binding.etNombre.setText(ejercicio.nombre)
        binding.etRepeticiones.setText(ejercicio.reps.toString())
        binding.etPeso.setText(ejercicio.peso.toString())
        binding.etDificultad.setText(ejercicio.dificultad.toString())

        title = "Editar: ${ejercicio.nombre}"
    }

    private fun observeViewModel() {
        viewModel.ejercicio.observe(this) { ejercicio ->
            if (ejercicio != null) {
                llenarCampos(ejercicio)
            } else if (idEjercicioExistente != null && viewModel.isLoading.value == false) {
                Toast.makeText(this, "No se pudo cargar el ejercicio.", Toast.LENGTH_LONG).show()
            }
        }

        viewModel.isLoading.observe(this) { isLoading ->
            // Deshabilita el botón mientras guarda
            binding.btnConfirmar.isEnabled = !isLoading
        }

        // Manejo de estado: Éxito (cierra la activity y notifica a la anterior)
        viewModel.operacionExitosa.observe(this) { message ->
            if (message != null) {
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
                viewModel.clearOperationStatus()
                setResult(RESULT_OK) // Señal de éxito para EjercicioActivity (para recargar)
                finish()
            }
        }

        // Manejo de estado: Error
        viewModel.error.observe(this) { error ->
            if (error != null) {
                Toast.makeText(this, error, Toast.LENGTH_LONG).show()
                viewModel.clearOperationStatus()
            }
        }
    }

    // FUNCIÓN PRINCIPAL: Recoge datos y llama al guardado en BD
    private fun guardarDatos() {
        // 1. Recoger y validar datos
        // 🔑 USAMOS LOS IDS SIN VERIFICACIÓN DE NULL
        val nombre = binding.etNombre.text.toString().trim()
        val repsStr = binding.etRepeticiones.text.toString().trim()
        val pesoStr = binding.etPeso.text.toString().trim()
        val dificultadStr = binding.etDificultad.text.toString().trim()

        if (nombre.isBlank() || repsStr.isBlank()) {
            Toast.makeText(this, "Por favor, rellena los campos obligatorios.", Toast.LENGTH_LONG).show()
            return
        }

        // 2. Conversión a tipos seguros (Int/Double)
        val reps = repsStr.toIntOrNull() ?: 0
        val peso = pesoStr.toDoubleOrNull() ?: 0.0
        val dificultad = dificultadStr.toDoubleOrNull() ?: 1.0

        if (reps <= 0) {
            Toast.makeText(this, "Las repeticiones deben ser un valor positivo válido.", Toast.LENGTH_LONG).show()
            return
        }

        // 3. Llamar al ViewModel para la inserción/actualización
        viewModel.guardarEjercicio(
            idEjercicio = idEjercicioExistente,
            idDiaRutina = idDiaRutina!!, // El ViewModel fallará si es null, pero la lógica lo exige
            nombre = nombre,
            reps = reps,
            peso = peso,
            dificultad = dificultad
        )
    }
}