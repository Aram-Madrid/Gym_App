package com.example.ut2_app.activities

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.ut2_app.databinding.ActivityLoginBinding
import com.example.ut2_app.util.SupabaseClientProvider
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import kotlinx.coroutines.launch
class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private val supabase = SupabaseClientProvider.supabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Si ya hay sesión activa, ir a MainActivity
        lifecycleScope.launch {
            if (supabase.auth.currentUserOrNull() != null) {
                startActivity(Intent(this@LoginActivity, MainActivity::class.java))
                finish()
            }
        }

        // Botón: Iniciar sesión
        binding.buttonIniciarSesion.setOnClickListener {
            val email = binding.editTextEmail.text.toString().trim()
            val password = binding.editTextPwd.text.toString().trim()

            if (email.isEmpty()) {
                binding.editTextEmail.error = "Completa todos los campos"
                return@setOnClickListener
            }
            if (password.isEmpty()) {
                binding.editTextPwd.error = "Completa todos los campos"
                return@setOnClickListener
            }

            iniciarSesionSupabase(email, password)
        }

        // Botón: Ir a crear cuenta
        binding.buttonCrearCuenta.setOnClickListener {
            startActivity(Intent(this, CrearCuentaActivity::class.java))
        }
    }

    private fun iniciarSesionSupabase(email: String, password: String) {
        lifecycleScope.launch {
            try {
                supabase.auth.signInWith(Email) {
                    this.email = email
                    this.password = password
                }

                Toast.makeText(
                    this@LoginActivity,
                    "Inicio de sesión exitoso",
                    Toast.LENGTH_SHORT
                ).show()

                startActivity(Intent(this@LoginActivity, MainActivity::class.java))
                finish()

            } catch (e: Exception) {
                Toast.makeText(
                    this@LoginActivity,
                    "Error: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
}