package com.example.ut2_app.activities

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.example.ut2_app.R
import com.example.ut2_app.databinding.ActivityMainBinding
import com.example.ut2_app.util.SupabaseClientProvider
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.decodeRecord
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.realtime
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

// Data class para la notificación
@Serializable
data class Notificacion(
    val id: String,
    val mensaje: String,
    val created_at: String,
    val id_usuario_destino: String
)

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private val supabase = SupabaseClientProvider.supabase

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            Log.d("PERMISOS", "Permiso de notificación concedido. Las alertas funcionarán.")
        } else {
            Log.w("PERMISOS", "Permiso de notificación DENEGADO. No se mostrarán las notificaciones locales.")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()

        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Lógica de Notificaciones
        requestNotificationPermission()
        createNotificationChannel()
        setupRankingNotificationListener()

        // Configurar NavController con BottomNavigationView
        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.fragment_container) as NavHostFragment
        val navController = navHostFragment.navController

        binding.bottomNavigation.setupWithNavController(navController)
        binding.bottomNavigation.selectedItemId = R.id.homeFragment

        // Si venimos desde EjercicioActivity, abrir MiRutinaFragment
        val destino = intent.getStringExtra("abrir_fragmento")
        if (destino == "mi_rutina" && savedInstanceState == null) {
            navController.navigate(R.id.miRutinaFragment)
        }
    }

    // --- LÓGICA DE PERMISOS ---

    private fun requestNotificationPermission() {
        // Solo necesario en Android 13 (TIRAMISU) y superiores (API 33+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                // Si el permiso no está concedido, lo solicitamos.
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    // --- LÓGICA DE NOTIFICACIONES Y REALTIME ---

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Alertas de Ranking"
            val descriptionText = "Notificaciones cuando un amigo te supera."
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel("RANKING_CHANNEL_ID", name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun setupRankingNotificationListener() {
        val userId = supabase.auth.currentUserOrNull()?.id

        if (userId == null) {
            Log.w("REALTIME_DEBUG", "Usuario no logueado. Listener no iniciado.")
            return
        }

        Log.d("REALTIME_DEBUG", "UserID Suscrito: $userId")

        lifecycleScope.launch {
            try {
                val channel = supabase.realtime.channel("ranking_notifications")

                val changeFlow = channel.postgresChangeFlow<PostgresAction.Insert>(
                    schema = "public"
                ) {
                    table = "notificaciones"
                }

                changeFlow.onEach { action ->
                    Log.i("REALTIME_EVENT", "Evento recibido: INSERT")

                    val notificacion = action.decodeRecord<Notificacion>()
                    Log.i("REALTIME_EVENT", "Destino del mensaje: ${notificacion.id_usuario_destino}")

                    // 🚨 Modificamos la lógica para filtrar correctamente
                    if (notificacion.id_usuario_destino == userId) {
                        showLocalNotification("Alerta: " + notificacion.mensaje)
                        Log.i("REALTIME_EVENT", "¡Notificación mostrada al usuario correcto!")
                    } else {
                        Log.i("REALTIME_EVENT", "Notificación ignorada (no es para este usuario).")
                    }

                }.launchIn(lifecycleScope)

                channel.subscribe()
                Log.d("REALTIME_DEBUG", "Suscripción al canal 'ranking_notifications' establecida.")

            } catch (e: Exception) {
                Log.e("REALTIME_ERROR", "Error crítico al configurar Realtime: ${e.message}", e)
            }
        }
    }

    private fun showLocalNotification(message: String) {
        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val notification = NotificationCompat.Builder(this, "RANKING_CHANNEL_ID")
            .setContentTitle("¡Alerta de Ranking!")
            .setContentText(message)
            .setSmallIcon(R.drawable.ic_launcher_foreground) // 💡 Usa tu propio ícono
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }

    // --- MANEJO DE INTENTS ---

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