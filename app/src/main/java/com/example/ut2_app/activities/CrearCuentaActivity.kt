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
// 🔑 IMPORTACIONES CLAVE
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
                // 1. Crear usuario en Auth (Sistema de seguridad)
                supabase.auth.signUpWith(Email) {
                    this.email = email
                    this.password = password
                }

                val authUser = supabase.auth.currentUserOrNull()

                if (authUser != null) {
                    // 2. Construir JSON seguro para insertar en tabla pública 'usuarios'
                    // Esto evita el error de serialización y conflicto de datos
                    val usuarioJson = buildJsonObject {
                        put("id", authUser.id)
                        put("nombre", nombre)
                        put("email", email)
                        // Datos opcionales
                        if (alturaStr.isNotEmpty()) put("altura", alturaStr.toInt())
                        if (pesoStr.isNotEmpty()) put("peso", pesoStr.toInt())

                        // 🔑 VALORES INICIALES HARDCORE (Desde Cero)
                        put("elo", 0)
                        put("rango", "Cobre")
                        put("ultimo_puntaje", 0)
                    }

                    // 3. Insertar en tabla usuarios
                    supabase.postgrest["usuarios"].insert(usuarioJson)

                    // 4. Inicializar sus puntos de grupo muscular a 0 (Para el gráfico)
                    // Llamamos al trigger que ya tienes o insertamos manualmente si fallara
                    // (El trigger 'inicializar_puntos_grupo' que tienes debería encargarse,
                    // pero por seguridad el insert del usuario es lo principal).

                    Toast.makeText(this@CrearCuentaActivity, "Cuenta creada. ¡Bienvenido!", Toast.LENGTH_SHORT).show()

                    // Ir a la App
                    startActivity(Intent(this@CrearCuentaActivity, MainActivity::class.java))
                    finish()

                } else {
                    Toast.makeText(this@CrearCuentaActivity, "Revisa tu email para confirmar.", Toast.LENGTH_LONG).show()
                }

            } catch (e: Exception) {
                // Manejo de error si el usuario ya existe o falla la red
                Toast.makeText(this@CrearCuentaActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
}