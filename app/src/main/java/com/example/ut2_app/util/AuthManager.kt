package com.example.ut2_app.util

import com.example.ut2_app.model.Usuario
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.util.Log

/**
 * Gestor centralizado de autenticación y datos del usuario.
 * Evita tener IDs hardcodeados en múltiples ViewModels.
 */
object AuthManager {

    private val supabase = SupabaseClientProvider.supabase

    /**
     * Obtiene el ID del usuario actualmente autenticado.
     * @return ID del usuario o null si no hay sesión activa
     */
    suspend fun getCurrentUserId(): String? = withContext(Dispatchers.IO) {
        try {
            supabase.auth.currentUserOrNull()?.id
        } catch (e: Exception) {
            Log.e("AuthManager", "Error obteniendo ID usuario: ${e.message}")
            null
        }
    }

    /**
     * Obtiene el ID del usuario de forma síncrona (solo si ya hay sesión cargada).
     * Usar solo cuando estés seguro de que la sesión está inicializada.
     */
    fun getCurrentUserIdSync(): String? {
        return try {
            supabase.auth.currentSessionOrNull()?.user?.id
        } catch (e: Exception) {
            Log.e("AuthManager", "Error obteniendo ID usuario sync: ${e.message}")
            null
        }
    }

    /**
     * Obtiene los datos completos del usuario desde la tabla 'usuarios'.
     * @return Objeto Usuario o null si hay error
     */
    suspend fun getCurrentUserData(): Usuario? = withContext(Dispatchers.IO) {
        try {
            val userId = getCurrentUserId() ?: return@withContext null

            supabase.postgrest["usuarios"]
                .select {
                    filter { eq("id", userId) }
                }
                .decodeSingleOrNull<Usuario>()
        } catch (e: Exception) {
            Log.e("AuthManager", "Error obteniendo datos usuario: ${e.message}")
            null
        }
    }

    /**
     * Verifica si hay una sesión activa.
     */
    suspend fun isUserLoggedIn(): Boolean {
        return getCurrentUserId() != null
    }

    /**
     * Cierra la sesión del usuario actual.
     */
    suspend fun signOut() {
        try {
            supabase.auth.signOut()
            Log.d("AuthManager", "Sesión cerrada correctamente")
        } catch (e: Exception) {
            Log.e("AuthManager", "Error al cerrar sesión: ${e.message}")
            throw e
        }
    }
}