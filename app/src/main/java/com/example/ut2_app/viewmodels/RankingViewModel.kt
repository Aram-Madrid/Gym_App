package com.example.ut2_app.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ut2_app.model.UsuarioRankingDB
import com.example.ut2_app.util.AuthManager
import com.example.ut2_app.util.SupabaseClientProvider
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.launch
import android.util.Log

class RankingViewModel : ViewModel() {

    private val _usuariosRanking = MutableLiveData<List<UsuarioRankingDB>>(emptyList())
    val usuariosRanking: LiveData<List<UsuarioRankingDB>> = _usuariosRanking

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    // ID del usuario logueado (se obtiene dinámicamente)
    private var currentUserId: String? = null

    init {
        // No cargar automáticamente, esperar a que el Fragment llame
    }

    fun cargarRanking() {
        viewModelScope.launch {
            _isLoading.postValue(true)
            try {
                // Obtener ID del usuario actual
                currentUserId = AuthManager.getCurrentUserId()

                if (currentUserId == null) {
                    Log.w("RankingViewModel", "No hay usuario autenticado")
                    _usuariosRanking.postValue(emptyList())
                    _isLoading.postValue(false)
                    return@launch
                }

                Log.d("RankingViewModel", "Cargando ranking para usuario: $currentUserId")

                val postgrestClient = SupabaseClientProvider.supabase.postgrest

                // Consulta: SELECT * FROM usuarios ORDER BY elo DESC
                val resultados = postgrestClient["usuarios"]
                    .select() {
                        order("elo", Order.DESCENDING)
                    }
                    .decodeList<UsuarioRankingDB>()

                // Procesar la lista para asignar la posición y marcar al usuario actual
                val listaProcesada = resultados.mapIndexed { index, usuario ->
                    usuario.copy(
                        posicion = index + 1,
                        esActual = usuario.id == currentUserId // Marcar al usuario logueado
                    )
                }

                _usuariosRanking.postValue(listaProcesada)
                Log.d("RankingViewModel", "Ranking cargado: ${listaProcesada.size} usuarios")

            } catch (e: Exception) {
                Log.e("RankingViewModel", "Error al cargar el ranking: ${e.message}", e)
                _usuariosRanking.postValue(emptyList())
            } finally {
                _isLoading.postValue(false)
            }
        }
    }
}