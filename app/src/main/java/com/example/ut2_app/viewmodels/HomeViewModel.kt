package com.example.ut2_app.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ut2_app.model.PuntosAcumulados
import com.example.ut2_app.model.PuntosGrupoUI
import com.example.ut2_app.util.AuthManager
import com.example.ut2_app.util.SupabaseClientProvider
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import android.util.Log
import io.github.jan.supabase.postgrest.query.Columns.Companion.list

class HomeViewModel : ViewModel() {

    private val _puntosRendimiento = MutableStateFlow<List<PuntosGrupoUI>>(emptyList())
    val puntosRendimiento: StateFlow<List<PuntosGrupoUI>> = _puntosRendimiento.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        cargarPuntos()
    }

    private fun cargarPuntos() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            try {
                // 🔑 MEJORA: Obtener ID del usuario dinámicamente
                val currentUserId = AuthManager.getCurrentUserId()

                if (currentUserId == null) {
                    Log.w("HomeViewModel", "No hay usuario autenticado")
                    _error.value = "No hay sesión activa"
                    _puntosRendimiento.value = emptyList()
                    return@launch
                }

                val postgrestClient = SupabaseClientProvider.supabase.postgrest
                val joinColumns = "grupo, puntos_acumulados, grupo_muscular_max(puntos_max)"

                val resultados = postgrestClient["usuario_puntos_grupo"]
                    .select(list(joinColumns)) {
                        filter {
                            eq("id_usuario", currentUserId)
                        }
                    }
                    .decodeList<PuntosAcumulados>()

                if (resultados.isEmpty()) {
                    Log.d("HomeViewModel", "No hay datos de rendimiento para el usuario")
                    _puntosRendimiento.value = emptyList()
                    return@launch
                }

                val listaMapeada = resultados.map { item ->
                    PuntosGrupoUI(
                        grupo = item.grupo,
                        valor = item.puntosAcumulados,
                        maximo = item.grupoMuscularMax.puntosMax
                    )
                }

                _puntosRendimiento.value = listaMapeada
                Log.d("HomeViewModel", "Datos cargados: ${listaMapeada.size} grupos musculares")

            } catch (e: Exception) {
                Log.e("HomeViewModel", "Error al cargar puntos de rendimiento: ${e.message}", e)
                _error.value = "Error al cargar datos: ${e.localizedMessage}"
                _puntosRendimiento.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Permite recargar los datos manualmente (por ejemplo, después de completar ejercicios)
     */
    fun recargarPuntos() {
        cargarPuntos()
    }
}