package com.example.ut2_app.activities

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.ut2_app.R
import com.example.ut2_app.adapters.EjercicioAdapter
import com.example.ut2_app.databinding.ActivityEjercicioBinding
import com.example.ut2_app.model.Ejercicio

class EjercicioActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEjercicioBinding
    private val listaEjercicios = mutableListOf<Ejercicio>()
    private lateinit var adapter: EjercicioAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEjercicioBinding.inflate(layoutInflater)
        setContentView(binding.root)

        //Mostrar el nombre del día recibido
        val nombreDia = intent.getStringExtra("nombre_dia")
        binding.textViewTitulo.text = "Rutina del $nombreDia"

        //Configurar RecyclerView
        adapter = EjercicioAdapter(listaEjercicios)
        binding.recyclerViewEjercicios.layoutManager = LinearLayoutManager(this)
        binding.recyclerViewEjercicios.adapter = adapter

        //Botón para agregar nuevo ejercicio
        binding.btnAgregarEjercicio.setOnClickListener {
            val intent = Intent(this, DetalleEjercicioActivity::class.java)
            startActivityForResult(intent, 1)
        }

        //Bóton para volver a MiRutinaFragment
        binding.botonVueltaRutina.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            intent.putExtra("abrir_mirutina", true)
            startActivity(intent)
            finish()
        }
    }

    //Recibir el resultado desde DetalleEjercicioActivity
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == 1 && resultCode == RESULT_OK && data != null) {
            val nuevoEjercicio = data.getSerializableExtra("nuevo_ejercicio") as? Ejercicio
            nuevoEjercicio?.let {
                listaEjercicios.add(it)
                adapter.notifyItemInserted(listaEjercicios.size - 1)
            }
        }
    }
}
