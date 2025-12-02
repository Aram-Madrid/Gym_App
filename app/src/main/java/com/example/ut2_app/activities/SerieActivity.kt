package com.example.ut2_app.activities

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.ut2_app.databinding.ActivitySerieBinding

class SerieActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySerieBinding

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySerieBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val nombreEjercicio = intent.getStringExtra("nombre_ejercicio")
        binding.textViewTitulo.text = "Series de: $nombreEjercicio"

        binding.btnFinalizar.setOnClickListener {
            finish()
        }
    }
}
