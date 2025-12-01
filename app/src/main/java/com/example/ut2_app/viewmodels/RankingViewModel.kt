package com.example.ut2_app.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ut2_app.model.UsuarioRankingDB
import com.example.ut2_app.util.SupabaseClientProvider
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.launch
import android.util.Log

class RankingViewModel : ViewModel() {

    private val _usuariosRanking = MutableLiveData<List<UsuarioRankingDB>>(emptyList())
    val usuariosRanking: LiveData<List<UsuarioRankingDB>> = _usuariosRanking

    // ⚠️ ID del usuario logueado (debes obtenerlo de Supabase Auth)
    private val currentUserId: String = "5e6ba799-6770-400d-b747-1989fe602ff2"

    init {
        cargarRanking()
    }

    fun cargarRanking() {
        viewModelScope.launch {
            try {
                val postgrestClient = SupabaseClientProvider.supabase.postgrest

                // Consulta: SELECT * FROM usuarios ORDER BY elo DESC
                val resultados = postgrestClient["usuarios"]
                    .select() {
                        order("elo", Order.DESCENDING)
                    }
                    .decodeList<UsuarioRankingDB>()

                // 🔑 1. Procesar la lista para asignar la posición y marcar al usuario actual
                val listaProcesada = resultados.mapIndexed { index, usuario ->
                    usuario.copy(
                        posicion = index + 1,
                        esActual = usuario.id == currentUserId // Marcar al usuario logueado
                    )
                }

                _usuariosRanking.postValue(listaProcesada)

            } catch (e: Exception) {
                Log.e("RankingViewModel", "Error al cargar el ranking: ${e.message}", e)
                _usuariosRanking.postValue(emptyList())
            }
        }
    }
}