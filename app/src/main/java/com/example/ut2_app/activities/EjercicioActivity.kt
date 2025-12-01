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

    // 🔑 1. OBTENER EL ID DEL DÍA (Ahora permite nulos y lo maneja en onCreate)
    private val idDiaRutina: String?
        get() = intent.getStringExtra("id_dia")

    private val nombreDia: String?
        get() = intent.getStringExtra("nombre_dia")

    // 🔑 2. INICIALIZAR EL VIEWMODEL CON LA FACTORÍA (Pasamos el ID, que puede ser nulo)
    private val viewModel: EjercicioViewModel by viewModels {
        EjercicioViewModelFactory(idDiaRutina.toString())
    }

    // 🔑 3. REGISTRO DEL LAUNCHER (Manejador de guardado)
    private val detalleEjercicioLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val data: Intent? = result.data
                val nuevoEjercicio = data?.getSerializableExtra("nuevo_ejercicio") as? Ejercicio

                nuevoEjercicio?.let { ejercicio ->
                    lifecycleScope.launch {
                        try {
                            // Guardar en Supabase (el ViewModel gestiona el ID de la BD)
                            viewModel.guardarEjercicio(ejercicio)

                            // Recargar la lista (sin argumentos).
                            viewModel.cargarEjercicios()

                            Toast.makeText(
                                this@EjercicioActivity,
                                "Ejercicio guardado!",
                                Toast.LENGTH_SHORT
                            ).show()
                        } catch (e: Exception) {
                            Toast.makeText(
                                this@EjercicioActivity,
                                "Error al guardar: ${e.message}",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                }
            }
        }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEjercicioBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Asumimos que el TextView se llama textViewTitulo
        binding.textViewTitulo.text = "Rutina del ${nombreDia ?: "Día"}"

        // Configurar RecyclerView
        adapter = EjercicioAdapter(mutableListOf())
        binding.recyclerViewEjercicios.layoutManager = LinearLayoutManager(this)
        binding.recyclerViewEjercicios.adapter = adapter

        // 🔑 LÓGICA DE CARGA: Si el ID existe, carga los ejercicios. Si no, muestra vacío.
        if (idDiaRutina != null) {
            viewModel.cargarEjercicios()
        } else {
            // Modo Creación: Muestra la lista vacía, lista para añadir el primer elemento.
            binding.recyclerViewEjercicios.visibility = View.VISIBLE
            // Puedes mostrar un mensaje de "Añade tu primer ejercicio" si lo deseas.
        }

        observeEjercicios()

        // Botón para agregar nuevo ejercicio
        binding.btnAgregarEjercicio.setOnClickListener {
            val intent = Intent(this, DetalleEjercicioActivity::class.java).apply {
                // PASAR EL ID DEL DÍA (Puede ser null si el usuario pulsa un día inactivo)
                putExtra("ID_DIA_RUTINA", idDiaRutina)
                putExtra("NOMBRE_DIA", nombreDia)
            }
            detalleEjercicioLauncher.launch(intent)
        }

        // Botón para volver a MiRutinaFragment
        binding.botonVueltaRutina.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            intent.putExtra("abrir_fragmento", "mi_rutina")
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            startActivity(intent)
        }
    }

    private fun observeEjercicios() {
        viewModel.listaEjercicios.observe(this) { nuevaLista ->
            // El adapter actualizará la lista, ya sea con ejercicios o con lista vacía
            (binding.recyclerViewEjercicios.adapter as EjercicioAdapter).actualizarLista(nuevaLista)
        }
    }
}