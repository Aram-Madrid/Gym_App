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
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

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

        binding.imgFotoPerfil.setImageResource(R.drawable.place_holder)

        val sharedPref = getSharedPreferences("AppPrefs", MODE_PRIVATE)
        modoOscuro = sharedPref.getBoolean("modoOscuro", false)

        if (modoOscuro) {
            binding.bolaSwitch.translationX = binding.fondoSwitch.width - binding.bolaSwitch.width - 8f
        }

        val fondoAnimado = binding.fondoSwitch.background as TransitionDrawable

        // Cambiar tema con animación del switch
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
            modoOscuro =!modoOscuro
        }

        // Click en botón confirmar
        binding.btnConfirmar.setOnClickListener {
            sharedPref.edit().putBoolean("modoOscuro", modoOscuro).apply()

            if (modoOscuro) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            }

            Toast.makeText(this, "Cambios aplicados", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }

        // CERRAR SESIÓN (Sin cambios)
        binding.btnCerrarSesion.setOnClickListener {
            lifecycleScope.launch {
                try {
                    supabase.auth.signOut()
                    Toast.makeText(this@ConfiguracionActivity, "Sesión cerrada correctamente", Toast.LENGTH_SHORT).show()
                    val intent = Intent(this@ConfiguracionActivity, LoginActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    finish()
                } catch (e: Exception) {
                    Toast.makeText(
                        this@ConfiguracionActivity,
                        "Error al cerrar sesión: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }

        // SUBIR FOTO DE PERFIL
        binding.imgFotoPerfil.setOnClickListener {
            galleryLauncher.launch("image/*")
        }
    }

    private fun setupGalleryLauncher() {
        galleryLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let {
                binding.imgFotoPerfil.setImageURI(it)
                subirFotoPerfil(it)
            }
        }
    }

    private fun subirFotoPerfil(uri: Uri) {
        val userId = supabase.auth.currentUserOrNull()?.id
        if (userId == null) {
            Toast.makeText(this, "Debe iniciar sesión para subir fotos.", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            try {
                // 1. Convertir URI de Android a un archivo temporal (Debe hacerse en un hilo de IO)
                val tempFile = withContext(Dispatchers.IO) {
                    uriToFile(uri, userId)
                }

                val bucketName = "profile_pictures" // Bucket solicitado
                val filePath = "$userId/avatar.jpg"

                // 2. Subir el archivo a Supabase Storage
                supabase.storage.from(bucketName)
                    .upload(filePath, tempFile) {
                        upsert = true
                    }

                // 3. Obtener la URL pública de la imagen
                val publicUrl = supabase.storage.from(bucketName)
                    .publicUrl(filePath)

                // 4. Actualizar el campo 'fotoperfilurl' en la tabla 'usuarios'
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
        val inputStream: InputStream? = contentResolver.openInputStream(uri)
        requireNotNull(inputStream) { "No se pudo abrir el InputStream para la URI." }

        val tempFile = File(cacheDir, "avatar_${userId}_temp.jpg")

        FileOutputStream(tempFile).use { outputStream ->
            inputStream.copyTo(outputStream)
        }
        return tempFile
    }
}