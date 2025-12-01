package com.example.ut2_app.activities

import android.animation.ObjectAnimator
import android.content.Intent
import android.graphics.drawable.TransitionDrawable
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.lifecycleScope // Importación necesaria
import com.example.ut2_app.R
import com.example.ut2_app.databinding.ActivityConfiguracionBinding
import com.example.ut2_app.util.SupabaseClientProvider // Importación necesaria
import io.github.jan.supabase.auth.auth // Importación necesaria
import kotlinx.coroutines.launch // Importación necesaria




class ConfiguracionActivity : AppCompatActivity() {

    private lateinit var binding: ActivityConfiguracionBinding
    private val supabase = SupabaseClientProvider.supabase // Cliente Supabase
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

        //... (lógica de modo oscuro sin cambios)

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

            modoOscuro =!modoOscuro
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

        // -------------------------------------------------------------
        // CERRAR SESIÓN (LOGIC CONVERTIDA A SUPABASE)
        // -------------------------------------------------------------
        binding.btnCerrarSesion.setOnClickListener {
            lifecycleScope.launch {
                try {
                    // Cierra la sesión activa de Supabase
                    supabase.auth.signOut()

                    Toast.makeText(this@ConfiguracionActivity, "Sesión cerrada correctamente", Toast.LENGTH_SHORT).show()

                    // Redirección a LoginActivity y limpia la pila de actividades
                    val intent = Intent(this@ConfiguracionActivity, LoginActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    finish()

                } catch (e: Exception) {
                    Toast.makeText(
                        this@ConfiguracionActivity,
                        "Error al cerrar sesión: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }

        // TODO: Implementar subida de fotos más adelante
        // Click en la imagen de perfil deshabilitado por ahora
        binding.imgFotoPerfil.setOnClickListener {
            Toast.makeText(this, "Subida de fotos deshabilitada temporalmente", Toast.LENGTH_SHORT).show()
        }
    }
}