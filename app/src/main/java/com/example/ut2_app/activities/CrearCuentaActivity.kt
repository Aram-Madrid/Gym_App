package com.example.ut2_app.activities

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.ut2_app.databinding.ActivityCrearCuentaBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class CrearCuentaActivity : AppCompatActivity() {
    private lateinit var binding: ActivityCrearCuentaBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCrearCuentaBinding.inflate(layoutInflater)
        setContentView(binding.root)

        //autenticacion y guardar datos
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        binding.buttonCrearCuenta.setOnClickListener {
            val email = binding.editTextEmail.text.toString().trim()
            val password = binding.editTextPwd.text.toString().trim()
            val nombre = binding.nombreUsuario.text.toString().trim()
            val altura = binding.alturaUsuario.text.toString().trim()
            val peso = binding.peso.text.toString().trim()

            if (email.isEmpty() || password.isEmpty() || nombre.isEmpty() || altura.isEmpty() || peso.isEmpty()) {
                Toast.makeText(this, "Por favor, completa todos los campos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Crear usuario en firebase
            auth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener { result ->
                    val userId = result.user?.uid ?: return@addOnSuccessListener

                    // Datos "Default" para el futuro
                    val datosUsuario = hashMapOf(
                        "nombre" to nombre,
                        "email" to email,
                        "altura" to altura,
                        "peso" to peso,
                        "elo" to 0,
                        "rango" to "Bronze",
                        "fotoPerfilUrl" to ""
                    )

                    

                    db.collection("usuarios").document(userId).set(datosUsuario)
                        .addOnSuccessListener {
                            Toast.makeText(this, "Cuenta creada correctamente", Toast.LENGTH_SHORT).show()

                            // coleccion vacia de amigos
                            val amigosRef = db.collection("usuarios")
                                .document(userId)
                                .collection("amigos")

                            amigosRef.add(hashMapOf("placeholder" to true))
                                .addOnSuccessListener { doc ->
                                    doc.delete() // elimina el placeholder
                                }
                                .addOnFailureListener { e ->
                                    Toast.makeText(this, "Error creando subcolección amigos: ${e.message}", Toast.LENGTH_SHORT).show()
                                }

                            // Ir a mainActivity
                            val intent = Intent(this, MainActivity::class.java)
                            startActivity(intent)
                            finish()
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(this, "Error al guardar los datos: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Error al crear la cuenta: Introduzca un correo electrónico válido", Toast.LENGTH_SHORT).show()
                }
        }
    }
}
