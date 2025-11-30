package com.example.ut2_app.activities

import android.animation.ObjectAnimator
import android.graphics.drawable.TransitionDrawable
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import android.content.Intent
import android.widget.Toast
import com.example.ut2_app.databinding.ActivityConfiguracionBinding
import com.google.firebase.auth.FirebaseAuth
import com.example.ut2_app.R


class ConfiguracionActivity : AppCompatActivity() {

    private lateinit var binding: ActivityConfiguracionBinding
    private var modoOscuro = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityConfiguracionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Forzar placeholder
        binding.imgFotoPerfil.setImageResource(R.drawable.place_holder)

        // Cargar modo oscuro desde SharedPreferences
        val sharedPref = getSharedPreferences("AppPrefs", MODE_PRIVATE)
        modoOscuro = sharedPref.getBoolean("modoOscuro", false)

        if (modoOscuro) {
            binding.bolaSwitch.translationX = binding.fondoSwitch.width - binding.bolaSwitch.width - 8f
        }

        val fondoAnimado = binding.fondoSwitch.background as TransitionDrawable

        // Cambiar tema con animación del switch
        binding.fondoSwitch.setOnClickListener {
            val bola = binding.bolaSwitch
            val moverA: Float

            if (!modoOscuro) {
                moverA = binding.fondoSwitch.width - bola.width - 8f
                ObjectAnimator.ofFloat(bola, "translationX", moverA).apply {
                    ObjectAnimator.setFrameDelay(300)
                    start()
                }
                fondoAnimado.startTransition(300)
            } else {
                moverA = 0f
                ObjectAnimator.ofFloat(bola, "translationX", moverA).apply {
                    ObjectAnimator.setFrameDelay(300)
                    start()
                }
                fondoAnimado.reverseTransition(300)
            }

            modoOscuro = !modoOscuro
        }

        // Click en botón confirmar
        binding.btnConfirmar.setOnClickListener {
            sharedPref.edit().putBoolean("modoOscuro", modoOscuro).apply()

            if (modoOscuro) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            }

            Toast.makeText(this, "Cambios aplicados", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }

        // Cerrar sesión
        binding.btnCerrarSesion.setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            Toast.makeText(this, "Sesión cerrada correctamente", Toast.LENGTH_SHORT).show()
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }

        // TODO: Implementar subida de fotos más adelante
        // Click en la imagen de perfil deshabilitado por ahora
        binding.imgFotoPerfil.setOnClickListener {
            Toast.makeText(this, "Subida de fotos deshabilitada temporalmente", Toast.LENGTH_SHORT).show()
        }
    }
}