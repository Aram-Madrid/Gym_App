package com.example.ut2_app.activities

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.example.ut2_app.R
import com.example.ut2_app.databinding.ActivityMainBinding
import android.content.Intent


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

        // Listener del menú inferior
        binding.bottomNavigation.setupWithNavController(navController)

        // Volver a dejar la selección inicial en Home
        binding.bottomNavigation.selectedItemId = R.id.homeFragment

        // Si venimos desde EjercicioActivity, abrir MiRutinaFragment
        val destino = intent.getStringExtra("abrir_fragmento")
        if (destino == "mi_rutina" && savedInstanceState == null){
            navController.navigate(R.id.miRutinaFragment)
        }
    }

    // Manejar nuevos intents cuando MainActivity ya estaba en la pila
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)

        val destino = intent.getStringExtra("abrir_fragmento")
        if (destino == "mi_rutina") {
            val navHostFragment =
                supportFragmentManager.findFragmentById(R.id.fragment_container) as NavHostFragment
            val navController = navHostFragment.navController
            navController.navigate(R.id.miRutinaFragment)
        }
    }

}
