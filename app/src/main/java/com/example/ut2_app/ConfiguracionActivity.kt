package com.example.ut2_app

import android.animation.ObjectAnimator
import android.graphics.drawable.TransitionDrawable
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.example.ut2_app.databinding.ActivityConfiguracionBinding

class ConfiguracionActivity : AppCompatActivity() {

    private lateinit var binding: ActivityConfiguracionBinding
    private var modoOscuro = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityConfiguracionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Fondo animado (transición claro <-> oscuro)
        val fondoAnimado = binding.fondoSwitch.background as TransitionDrawable

        binding.fondoSwitch.setOnClickListener {
            val bola = binding.bolaSwitch
            val moverA: Float

            if (!modoOscuro) {
                // Mover bolita a la derecha
                moverA = binding.fondoSwitch.width - bola.width - 8f
                ObjectAnimator.ofFloat(bola, "translationX", moverA).apply {
                    duration = 300
                    start()
                }

                // Animar fondo y cambiar tema
                fondoAnimado.startTransition(300)
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            } else {
                // Mover bolita a la izquierda
                moverA = 0f
                ObjectAnimator.ofFloat(bola, "translationX", moverA).apply {
                    duration = 300
                    start()
                }

                // Volver fondo y tema claro
                fondoAnimado.reverseTransition(300)
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            }

            // Cambiar estado
            modoOscuro = !modoOscuro
        }
    }
}
