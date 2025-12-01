package com.example.ut2_app.activities

import android.animation.ObjectAnimator
import android.content.Intent
import android.graphics.drawable.TransitionDrawable
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.lifecycleScope
import coil.load // Usaremos COIL para cargar la foto de perfil (si no usas Coil, reemplázalo por Glide/Picasso)
import com.example.ut2_app.R
import com.example.ut2_app.databinding.ActivityConfiguracionBinding
import com.example.ut2_app.util.SupabaseClientProvider
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.storage.storage
import io.github.jan.supabase.storage.upload
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

// Data class para cargar los datos iniciales
@Serializable
data class UserProfile(
    val nombre: String? = null,
    val fotoperfilurl: String? = null
)

class ConfiguracionActivity : AppCompatActivity() {

    private lateinit var binding: ActivityConfiguracionBinding
    private val supabase = SupabaseClientProvider.supabase
    private var modoOscuro = false

    private lateinit var galleryLauncher: ActivityResultLauncher<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityConfiguracionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupGalleryLauncher()
        cargarDatosIniciales() // NUEVA LLAMADA PARA CARGAR NOMBRE Y FOTO

        // --- Lógica del Switch de Tema ---
        val sharedPref = getSharedPreferences("AppPrefs", MODE_PRIVATE)
        modoOscuro = sharedPref.getBoolean("modoOscuro", false)

        if (modoOscuro) {
            // Se usa post para asegurar que el layout está medido
            binding.fondoSwitch.post {
                binding.bolaSwitch.translationX = binding.fondoSwitch.width - binding.bolaSwitch.width - 8f
            }
        }

        val fondoAnimado = binding.fondoSwitch.background as TransitionDrawable

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
            modoOscuro = !modoOscuro
        }
        // ----------------------------------

        // Click en botón confirmar (Añadimos lógica de cambio de nombre)
        binding.btnConfirmar.setOnClickListener {
            val nuevoNombre = binding.editTextNombre.text.toString().trim()
            actualizarPerfilYTema(nuevoNombre) // LÓGICA DE ACTUALIZACIÓN COMBINADA
        }

        // CERRAR SESIÓN (Sin cambios)
        binding.btnCerrarSesion.setOnClickListener {
            // ... (código para cerrar sesión)
            lifecycleScope.launch {
                try {
                    supabase.auth.signOut()
                    Toast.makeText(this@ConfiguracionActivity, "Sesión cerrada correctamente", Toast.LENGTH_SHORT).show()
                    val intent = Intent(this@ConfiguracionActivity, LoginActivity::class.java) // ASUMIENDO LoginActivity
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    finish()
                } catch (e: Exception) {
                    Toast.makeText(this@ConfiguracionActivity, "Error al cerrar sesión: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }

        // SUBIR FOTO DE PERFIL (Sin cambios)
        binding.imgFotoPerfil.setOnClickListener {
            galleryLauncher.launch("image/*")
        }
    }

    // --- NUEVAS FUNCIONES Y MODIFICACIONES ---

    private fun cargarDatosIniciales() {
        val userId = supabase.auth.currentUserOrNull()?.id ?: return

        lifecycleScope.launch {
            try {
                val profile = supabase.postgrest["usuarios"]
                    .select { filter { eq("id", userId) } }
                    .decodeSingle<UserProfile>()

                // Cargar Nombre
                binding.editTextNombre.setText(profile.nombre)

                // Cargar Foto (Usando Coil para evitar problemas de caché, similar al RankingAdapter)
                if (!profile.fotoperfilurl.isNullOrEmpty()) {
                    val url = profile.fotoperfilurl
                    val cacheBustingUrl = "$url?v=${System.currentTimeMillis()}"

                    binding.imgFotoPerfil.load(cacheBustingUrl) {
                        crossfade(true)
                        error(com.example.ut2_app.R.drawable.place_holder)
                        placeholder(com.example.ut2_app.R.drawable.place_holder)
                    }
                } else {
                    binding.imgFotoPerfil.setImageResource(com.example.ut2_app.R.drawable.place_holder)
                }

            } catch (e: Exception) {
                Toast.makeText(this@ConfiguracionActivity, "Error al cargar perfil: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun actualizarPerfilYTema(nuevoNombre: String) {
        val userId = supabase.auth.currentUserOrNull()?.id ?: return
        val sharedPref = getSharedPreferences("AppPrefs", MODE_PRIVATE)

        lifecycleScope.launch {
            var nombreActualizado = false

            // Lógica de Actualización de Nombre
            if (nuevoNombre.isNotEmpty()) {
                try {
                    supabase.postgrest["usuarios"]
                        .update(mapOf("nombre" to nuevoNombre)) {
                            filter { eq("id", userId) }
                        }
                    nombreActualizado = true
                } catch (e: Exception) {
                    Toast.makeText(this@ConfiguracionActivity, "Error al actualizar nombre: ${e.message}", Toast.LENGTH_LONG).show()
                    return@launch
                }
            }

            // Lógica de Actualización de Tema
            sharedPref.edit().putBoolean("modoOscuro", modoOscuro).apply()

            if (modoOscuro) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            }

            if (nombreActualizado) {
                Toast.makeText(this@ConfiguracionActivity, "Nombre y tema actualizados.", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this@ConfiguracionActivity, "Tema actualizado.", Toast.LENGTH_SHORT).show()
            }

            // Navegar de vuelta
            startActivity(Intent(this@ConfiguracionActivity, MainActivity::class.java)) // ASUMIENDO MainActivity
            finish()
        }
    }

    // --- MÉTODOS DE SUBIDA DE FOTO ---

    private fun setupGalleryLauncher() {
        // ... (código existente)
        galleryLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let {
                binding.imgFotoPerfil.setImageURI(it)
                subirFotoPerfil(it)
            }
        }
    }

    private fun subirFotoPerfil(uri: Uri) {
        // ... (código existente)
        val userId = supabase.auth.currentUserOrNull()?.id
        if (userId == null) {
            Toast.makeText(this, "Debe iniciar sesión para subir fotos.", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            try {
                val tempFile = withContext(Dispatchers.IO) {
                    uriToFile(uri, userId)
                }

                val bucketName = "profile_pictures"
                val filePath = "$userId/avatar.jpg"

                supabase.storage.from(bucketName)
                    .upload(filePath, tempFile) {
                        upsert = true
                    }

                val publicUrl = supabase.storage.from(bucketName)
                    .publicUrl(filePath)

                supabase.postgrest["usuarios"]
                    .update(
                        mapOf("fotoperfilurl" to publicUrl)
                    ) {
                        filter { eq("id", userId) }
                    }

                Toast.makeText(this@ConfiguracionActivity, "Foto de perfil actualizada.", Toast.LENGTH_SHORT).show()

            } catch (e: Exception) {
                Toast.makeText(this@ConfiguracionActivity, "Error al subir la foto: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun uriToFile(uri: Uri, userId: String): File {
        // ... (código existente)
        val inputStream: InputStream? = contentResolver.openInputStream(uri)
        requireNotNull(inputStream) { "No se pudo abrir el InputStream para la URI." }

        val tempFile = File(cacheDir, "avatar_${userId}_temp.jpg")

        FileOutputStream(tempFile).use { outputStream ->
            inputStream.copyTo(outputStream)
        }
        return tempFile
    }
}