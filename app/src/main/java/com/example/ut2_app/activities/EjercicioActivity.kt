package com.example.ut2_app.activities

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.ut2_app.adapters.EjercicioAdapter
import com.example.ut2_app.databinding.ActivityEjercicioBinding
import com.example.ut2_app.model.Ejercicio
import com.example.ut2_app.viewmodels.EjercicioViewModel
import com.example.ut2_app.viewmodels.EjercicioViewModelFactory
import kotlinx.coroutines.launch

class EjercicioActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEjercicioBinding
    private lateinit var adapter: EjercicioAdapter

    // 🔑 CORRECCIÓN 1: Obtener el ID como String? (puede ser null)
    private val idDiaRutina: String?
        get() = intent.getStringExtra("id_dia")

    private val nombreDia: String?
        get() = intent.getStringExtra("nombre_dia")

    // 🔑 CORRECCIÓN 2: Pasar null como null, NO como string "null"
    private val viewModel: EjercicioViewModel by viewModels {
        EjercicioViewModelFactory(idDiaRutina) // ✅ Pasamos el valor real (null o String)
    }

    // Launcher para el resultado de DetalleEjercicioActivity
    private val detalleEjercicioLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                // Recargar la lista después de guardar un ejercicio
                viewModel.cargarEjercicios()

                Toast.makeText(
                    this@EjercicioActivity,
                    "Ejercicio guardado correctamente",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEjercicioBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Configurar título
        binding.textViewTitulo.text = "Rutina del ${nombreDia ?: "Día"}"

        // Configurar RecyclerView
        adapter = EjercicioAdapter(mutableListOf())
        binding.recyclerViewEjercicios.layoutManager = LinearLayoutManager(this)
        binding.recyclerViewEjercicios.adapter = adapter

        // Observar ejercicios
        observeEjercicios()

        // Botón para agregar nuevo ejercicio
        binding.btnAgregarEjercicio.setOnClickListener {
            abrirDetalleEjercicio()
        }

        // Botón para volver a MiRutinaFragment
        binding.botonVueltaRutina.setOnClickListener {
            volverAMiRutina()
        }
    }

    private fun observeEjercicios() {
        viewModel.listaEjercicios.observe(this) { nuevaLista ->
            adapter.actualizarLista(nuevaLista)

            // Mostrar mensaje si la lista está vacía
            if (nuevaLista.isEmpty()) {
                // Puedes mostrar un TextView con "No hay ejercicios aún"
                // o simplemente dejar el RecyclerView vacío
            }
        }
    }

    private fun abrirDetalleEjercicio() {
        val intent = Intent(this, DetalleEjercicioActivity::class.java).apply {
            // Pasamos el ID del día (puede ser null si es nueva rutina)
            putExtra("ID_DIA_RUTINA", idDiaRutina)
            putExtra("NOMBRE_DIA", nombreDia)
        }
        detalleEjercicioLauncher.launch(intent)
    }

    private fun volverAMiRutina() {
        val intent = Intent(this, MainActivity::class.java).apply {
            putExtra("abrir_fragmento", "mi_rutina")
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        startActivity(intent)
    }
}