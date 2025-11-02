package com.example.ut2_app

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.ut2_app.databinding.ActivityLoginBinding

class LoginActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLoginBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)


        //boton de iniciar sesion
        binding.buttonIniciarSesion.setOnClickListener {
            val email = binding.editTextEmail.text.toString()
            val password = binding.editTextPwd.text.toString()

            if(email.isEmpty()){
                binding.editTextEmail.error = "Completa todos los campos"
            } else if (password.isEmpty()){
                binding.editTextPwd.error = "Completa todos los campos"
            } else {
                //ir a home
                val intent = Intent(this, MainActivity::class.java)
                startActivity(intent)
                finish()
            }
        }

        binding.buttonCrearCuenta.setOnClickListener{
            //ir a Crear cuenta
            val intent = Intent(this, CrearCuentaActivity::class.java)
            startActivity(intent)
        }


    }
}