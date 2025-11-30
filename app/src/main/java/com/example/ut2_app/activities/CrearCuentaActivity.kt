package com.example.ut2_app.activities

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.ut2_app.databinding.ActivityCrearCuentaBinding
import com.example.ut2_app.model.Usuario
import com.example.ut2_app.util.SupabaseClientProvider
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.postgrest.postgrest
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

            if (email.isEmpty() || password.isEmpty() || nombre.isEmpty() ||
                alturaStr.isEmpty() || pesoStr.isEmpty()
            ) {
                Toast.makeText(
                    this,
                    "Por favor, completa todos los campos",
                    Toast.LENGTH_SHORT
                ).show()
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
                // 1. Crear usuario en Auth
                supabase.auth.signUpWith(Email) {
                    this.email = email
                    this.password = password
                }

                // 2. Obtener el usuario recién creado
                val authUser = supabase.auth.currentUserOrNull()

                if (authUser != null) {

                    // 3. Construir el objeto Usuario
                    val usuario = Usuario(
                        id = authUser.id,
                        nombre = nombre,
                        email = email,
                        altura = alturaStr.toIntOrNull(),
                        peso = pesoStr.toIntOrNull()
                    )

                    // 4. Guardarlo en la tabla "usuarios"
                    supabase.postgrest["usuarios"].insert(usuario)

                    Toast.makeText(
                        this@CrearCuentaActivity,
                        "Cuenta creada correctamente",
                        Toast.LENGTH_SHORT
                    ).show()

                    startActivity(Intent(this@CrearCuentaActivity, MainActivity::class.java))
                    finish()

                } else {
                    Toast.makeText(
                        this@CrearCuentaActivity,
                        "Verifica tu correo e inicia sesión",
                        Toast.LENGTH_LONG
                    ).show()
                }

            } catch (e: Exception) {
                Toast.makeText(
                    this@CrearCuentaActivity,
                    "Error creando cuenta: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
}
