package com.example.ut2_app.activities

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.ut2_app.adapters.EjercicioAdapter
import com.example.ut2_app.databinding.ActivityEjercicioBinding
import com.example.ut2_app.viewmodels.EjercicioViewModel
import com.example.ut2_app.viewmodels.EjercicioViewModelFactory

class EjercicioActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEjercicioBinding
    private lateinit var adapter: EjercicioAdapter

    private val idDiaRutina: String?
        get() = intent.getStringExtra("id_dia")

    private val nombreDia: String?
        get() = intent.getStringExtra("nombre_dia")

    private val viewModel: EjercicioViewModel by viewModels {
        EjercicioViewModelFactory(idDiaRutina) // ✅ Pasamos el valor real (null o String)
    }

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