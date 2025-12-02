package com.example.ut2_app.activities

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.ut2_app.databinding.ActivityCrearCuentaBinding
import com.example.ut2_app.util.SupabaseClientProvider
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.coroutines.launch

class CrearCuentaActivity : AppCompatActivity() {
    private lateinit var binding: ActivityCrearCuentaBinding
    private val supabase = SupabaseClientProvider.supabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCrearCuentaBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.buttonCrearCuenta.setOnClickListener {
            val email = binding.editTextEmail.text.toString().trim()
            val password = binding.editTextPwd.text.toString().trim()
            val nombre = binding.nombreUsuario.text.toString().trim()
            val alturaStr = binding.alturaUsuario.text.toString().trim()
            val pesoStr = binding.peso.text.toString().trim()

            if (email.isEmpty() || password.isEmpty() || nombre.isEmpty()) {
                Toast.makeText(this, "Completa los campos obligatorios", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            crearCuentaSupabase(email, password, nombre, alturaStr, pesoStr)
        }
    }

    private fun crearCuentaSupabase(
        email: String,
        password: String,
        nombre: String,
        alturaStr: String,
        pesoStr: String
    ) {
        lifecycleScope.launch {
            try {
                supabase.auth.signUpWith(Email) {
                    this.email = email
                    this.password = password
                }

                val authUser = supabase.auth.currentUserOrNull()

                if (authUser != null) {
                    val usuarioJson = buildJsonObject {
                        put("id", authUser.id)
                        put("nombre", nombre)
                        put("email", email)
                        // Datos opcionales
                        if (alturaStr.isNotEmpty()) put("altura", alturaStr.toInt())
                        if (pesoStr.isNotEmpty()) put("peso", pesoStr.toInt())
                        put("elo", 0)
                        put("rango", "Cobre")
                        put("ultimo_puntaje", 0)
                    }

                    //Inserta en tabla usuarios
                    supabase.postgrest["usuarios"].insert(usuarioJson)

                    Toast.makeText(this@CrearCuentaActivity, "Cuenta creada. ¡Bienvenido!", Toast.LENGTH_SHORT).show()

                    startActivity(Intent(this@CrearCuentaActivity, MainActivity::class.java))
                    finish()

                } else {
                    Toast.makeText(this@CrearCuentaActivity, "Revisa tu email para confirmar.", Toast.LENGTH_LONG).show()
                }

            } catch (e: Exception) {
                // Manejo de error
                Toast.makeText(this@CrearCuentaActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
}