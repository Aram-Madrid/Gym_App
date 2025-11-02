package com.example.ut2_app

import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.ut2_app.databinding.ActivityCrearCuentaBinding

class CrearCuentaActivity : AppCompatActivity() {
    private lateinit var binding: ActivityCrearCuentaBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


        binding = ActivityCrearCuentaBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.buttonCrearCuenta.setOnClickListener {
            val email = binding.editTextEmail.text.toString()
            val password = binding.editTextPwd.text.toString()
            val nombre = binding.nombreUsuario.text.toString()
            val altura = binding.alturaUsuario.text.toString()
            val peso = binding.peso.text.toString()

            if (email.isEmpty() || password.isEmpty() || nombre.isEmpty() || altura.isEmpty() || peso.isEmpty()) {
                Toast.makeText(this, "Por favor, completa todos los campos", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Cuenta creada correctamente", Toast.LENGTH_SHORT).show()
                finish()
            }


        }





    }
}