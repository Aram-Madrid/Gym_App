package com.example.ut2_app.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ut2_app.model.PuntosAcumulados
import com.example.ut2_app.model.PuntosGrupoUI
import com.example.ut2_app.util.SupabaseClientProvider
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import android.util.Log // Para ver errores en Logcat
import io.github.jan.supabase.postgrest.query.Columns.Companion.list

class HomeViewModel : ViewModel() {

    // 🔑 ID del usuario inyectado
    private val currentUserId: String = "5e6ba799-6770-400d-b747-1989fe602ff2"

    private val _puntosRendimiento = MutableStateFlow<List<PuntosGrupoUI>>(emptyList())
    val puntosRendimiento: StateFlow<List<PuntosGrupoUI>> = _puntosRendimiento.asStateFlow()

    init {
        cargarPuntos()
    }

    private fun cargarPuntos() {
        viewModelScope.launch {
            try {
                val postgrestClient = SupabaseClientProvider.supabase.postgrest

                val joinColumns = "grupo, puntos_acumulados, grupo_muscular_max(puntos_max)"

                val resultados = postgrestClient["usuario_puntos_grupo"]
                    .select(list(joinColumns)) {
                        filter {
                            eq("id_usuario", currentUserId) // ⬅️ Filtro usando el ID real
                        }
                    }
                    // Asumiendo que tus modelos usan Float
                    .decodeList<PuntosAcumulados>()

                val listaMapeada = resultados.map { item ->
                    PuntosGrupoUI(
                        grupo = item.grupo,
                        valor = item.puntosAcumulados,
                        maximo = item.grupoMuscularMax.puntosMax
                    )
                }

                _puntosRendimiento.value = listaMapeada
                Log.d("HomeViewModel", "Datos cargados: ${listaMapeada.size} grupos.")

            } catch (e: Exception) {
                // 🚨 Revisaremos este Logcat si el gráfico sigue sin aparecer.
                Log.e("HomeViewModel", "Error al cargar puntos de rendimiento: ${e.message}", e)
                _puntosRendimiento.value = emptyList()
            }
        }
    }
}