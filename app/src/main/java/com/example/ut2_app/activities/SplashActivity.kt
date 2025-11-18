package com.example.ut2_app.activities

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.ut2_app.R

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

        // Esperar 2 segundos y abrir LoginActivity
        Handler(Looper.getMainLooper()).postDelayed({
            val intent = Intent(this, LoginActivity::class.java) // Cambia a MainActivity si quieres
            startActivity(intent)
            finish()
        }, 2000)
    }
}
