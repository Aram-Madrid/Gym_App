package com.example.ut2_app

import android.animation.ObjectAnimator
import android.graphics.drawable.TransitionDrawable
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.example.ut2_app.databinding.ActivityConfiguracionBinding
import com.google.firebase.auth.FirebaseAuth
import android.content.Intent
import android.widget.Toast
import android.content.Context

class ConfiguracionActivity : AppCompatActivity() {

    private lateinit var binding: ActivityConfiguracionBinding
    private var modoOscuro = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityConfiguracionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val sharedPref = getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
        modoOscuro = sharedPref.getBoolean("modoOscuro", false)

        if (modoOscuro) {
            binding.bolaSwitch.translationX = binding.fondoSwitch.width - binding.bolaSwitch.width - 8f
        }

        val fondoAnimado = binding.fondoSwitch.background as TransitionDrawable

        binding.fondoSwitch.setOnClickListener {
            val bola = binding.bolaSwitch
            val moverA: Float

            if (!modoOscuro) {
                moverA = binding.fondoSwitch.width - bola.width - 8f
                ObjectAnimator.ofFloat(bola, "translationX", moverA).apply {
                    duration = 300
                    start()
                }
                fondoAnimado.startTransition(300)
            } else {
                moverA = 0f
                ObjectAnimator.ofFloat(bola, "translationX", moverA).apply {
                    duration = 300
                    start()
                }
                fondoAnimado.reverseTransition(300)
            }

            modoOscuro = !modoOscuro
        }

        binding.btnConfirmar.setOnClickListener {
            sharedPref.edit().putBoolean("modoOscuro", modoOscuro).apply()

            // Aplicar tema ahora
            if (modoOscuro) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            }

            Toast.makeText(this, "Cambios aplicados", Toast.LENGTH_SHORT).show()

            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }

        binding.btnCerrarSesion.setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            Toast.makeText(this, "Sesión cerrada correctamente", Toast.LENGTH_SHORT).show()

            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }
}
