package com.example.ut2_app.activities

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.example.ut2_app.R
import com.example.ut2_app.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        // Instala la SplashScreen antes de setContentView
        installSplashScreen()

        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Configurar NavController con BottomNavigationView
        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.fragment_container) as NavHostFragment
        val navController = navHostFragment.navController

        // Si venimos desde EjercicioActivity, abrir MiRutinaFragment
        val abrirMiRutina = intent.getBooleanExtra("abrir_mirutina", false)
        if (abrirMiRutina && savedInstanceState == null) {
            navController.navigate(R.id.miRutinaFragment)
        }

        // Listener del menú inferior
        binding.bottomNavigation.setupWithNavController(navController)
    }
}
