package com.example.ut2_app

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.example.ut2_app.databinding.FragmentRankingBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore

class RankingFragment : Fragment() {

    private var _binding: FragmentRankingBinding? = null
    private val binding get() = _binding!!

    private val db = FirebaseFirestore.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRankingBinding.inflate(inflater, container, false)

        binding.btnAgregarAmigo.setOnClickListener {
            val builder = AlertDialog.Builder(requireContext())
            builder.setTitle("Agregar amigo")



            val input = EditText(requireContext())
            input.hint = "Correo del amigo"
            builder.setView(input)

            builder.setPositiveButton("Agregar") { dialog, _ ->
                val texto = input.text.toString().trim()
                if (texto.isNotEmpty()) {
                    agregarAmigo(texto)
                } else {
                    Toast.makeText(requireContext(), "Introduce un correo", Toast.LENGTH_SHORT).show()
                }
                dialog.dismiss()
            }

            builder.setNegativeButton("Cancelar") { dialog, _ -> dialog.dismiss() }
            builder.show()
        }

        cargarRankingAmigos()
        return binding.root
    }

    private fun cargarRankingAmigos() {
        val uidActual = FirebaseAuth.getInstance().currentUser?.uid
        val usuarios = mutableListOf<Usuario>()
        val adapter = RankingAdapter(requireContext(), usuarios)
        binding.listaRanking.adapter = adapter

        if (uidActual == null) {
            Toast.makeText(requireContext(), "Error: usuario no autenticado", Toast.LENGTH_SHORT).show()
            return
        }

        // Primero obtenemos los amigos
        db.collection("usuarios").document(uidActual)
            .collection("amigos")
            .get()
            .addOnSuccessListener { amigosDocs ->
                usuarios.clear()
                val friendIds = amigosDocs.mapNotNull { it.getString("amigoId") }.toMutableList()

                // Incluimos al propio usuario
                friendIds.add(uidActual)

                if (friendIds.isEmpty()) {
                    Toast.makeText(requireContext(), "Aún no tienes amigos añadidos", Toast.LENGTH_SHORT).show()
                    adapter.notifyDataSetChanged()
                    return@addOnSuccessListener
                }

                // Cargar todos los usuarios (amigos + yo)
                db.collection("usuarios")
                    .whereIn(FieldPath.documentId(), friendIds)
                    .get()
                    .addOnSuccessListener { result ->
                        for (doc in result) {
                            val id = doc.id
                            val nombre = doc.getString("nombre") ?: "Sin nombre"
                            val puntuacion = doc.getLong("elo")?.toInt() ?: 0
                            val fotoUrl = doc.getString("fotoUrl")
                            val esActual = id == uidActual
                            usuarios.add(Usuario(nombre, puntuacion, fotoUrl, 0, esActual))
                        }

                        // Ordenar por puntuación y asignar posiciones
                        usuarios.sortByDescending { it.puntuacion }
                        usuarios.forEachIndexed { index, usuario ->
                            usuario.posicion = index + 1
                        }

                        adapter.notifyDataSetChanged()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(requireContext(), "Error al cargar ranking: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Error al cargar amigos: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun agregarAmigo(emailAmigo: String) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        db.collection("usuarios")
            .whereEqualTo("email", emailAmigo)
            .get()
            .addOnSuccessListener { result ->
                if (result.isEmpty) {
                    Toast.makeText(requireContext(), "No existe un usuario con ese correo", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }

                val amigoDoc = result.documents.first()
                val amigoId = amigoDoc.id
                val amigoNombre = amigoDoc.getString("nombre") ?: "Desconocido"

                if (amigoId == userId) {
                    Toast.makeText(requireContext(), "No puedes agregarte a ti mismo", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }

                val amigoData = hashMapOf(
                    "amigoId" to amigoId,
                    "nombre" to amigoNombre
                )

                db.collection("usuarios").document(userId)
                    .collection("amigos")
                    .document(amigoId)
                    .set(amigoData)
                    .addOnSuccessListener {
                        Toast.makeText(requireContext(), "$amigoNombre añadido a tus amigos", Toast.LENGTH_SHORT).show()
                        cargarRankingAmigos()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(requireContext(), "Error al agregar amigo: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Error al buscar usuario: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
