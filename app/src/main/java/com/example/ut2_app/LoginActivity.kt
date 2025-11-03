package com.example.ut2_app

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.ut2_app.databinding.ActivityCrearCuentaBinding
import com.example.ut2_app.databinding.ActivityLoginBinding
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException


class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FirebaseApp.initializeApp(this)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Inicializamos Firebase Auth
        auth = FirebaseAuth.getInstance()

        //Si ya se inicio sesion directamente ir a home
        if (FirebaseAuth.getInstance().currentUser != null){
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }

        // Botón Iniciar Sesión
        binding.buttonIniciarSesion.setOnClickListener {
            val email = binding.editTextEmail.text.toString().trim()
            val password = binding.editTextPwd.text.toString().trim()

            if (email.isEmpty()) {
                binding.editTextEmail.error = "Completa todos los campos"
            } else if (password.isEmpty()) {
                binding.editTextPwd.error = "Completa todos los campos"
            } else {
                iniciarSesion(email, password)
            }
        }

        // Botón Crear Cuenta
        binding.buttonCrearCuenta.setOnClickListener {
            val intent = Intent(this, CrearCuentaActivity::class.java)
            startActivity(intent)
        }
    }

    private fun iniciarSesion(email: String, password: String) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    // todo bem
                    Toast.makeText(this, "Inicio de sesión exitoso", Toast.LENGTH_SHORT).show()
                    //Vamos a mainActivity
                    val intent = Intent(this, MainActivity::class.java)
                    startActivity(intent)
                    finish()
                } else {
                    // Switch con el error
                    val exception = task.exception
                    when (exception) {
                        //no esta esa cuenta
                        is FirebaseAuthInvalidUserException -> {
                            Toast.makeText(
                                this,
                                "No existe una cuenta con ese correo",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                        //Se equivoco con la contraseña LOL
                        is FirebaseAuthInvalidCredentialsException -> {
                            Toast.makeText(
                                this,
                                "Contraseña incorrecta",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                        else -> {
                            //Generado por chatgpt porque no sabia que más podia ir mal
                            Toast.makeText(
                                this,
                                "Error al iniciar sesión: ${exception?.message}",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
            }
    }
}
