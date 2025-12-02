package com.example.ut2_app.activities

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.ut2_app.R
import com.example.ut2_app.util.SupabaseClientProvider
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.launch

class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        val logo = findViewById<ImageView>(R.id.logoSplash)
        val title = findViewById<TextView>(R.id.titleSplash)

        // Animación fade + zoom
        val fadeZoom = AnimationUtils.loadAnimation(this, R.anim.fade_zoom)
        logo.startAnimation(fadeZoom)
        title.startAnimation(fadeZoom)

        // Esperar 2 segundos y comprobar la sesión
        Handler(Looper.getMainLooper()).postDelayed({
            checkSessionAndNavigate()
        }, 2000)
    }

    private fun checkSessionAndNavigate() {
        lifecycleScope.launch {
            // 🔑 LÓGICA DE SESIÓN PERSISTENTE
            // Comprobamos si Supabase tiene un usuario guardado en local
            val usuarioActual = SupabaseClientProvider.supabase.auth.currentUserOrNull()

            if (usuarioActual != null) {
                // Si hay usuario, vamos directo al Home
                val intent = Intent(this@SplashActivity, MainActivity::class.java)
                startActivity(intent)
            } else {
                // Si no hay usuario, vamos al Login
                val intent = Intent(this@SplashActivity, LoginActivity::class.java)
                startActivity(intent)
            }
            // Cerramos el Splash para que no se pueda volver atrás
            finish()
        }
    }
}