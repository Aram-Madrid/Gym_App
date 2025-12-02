package com.example.ut2_app.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ut2_app.model.Maximos
import com.example.ut2_app.model.PuntosAcumulados
import com.example.ut2_app.model.PuntosGrupoUI
import com.example.ut2_app.util.AuthManager
import com.example.ut2_app.util.SupabaseClientProvider
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns.Companion.list
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * ViewModel para el HomeFragment.
 *
 * Gestiona:
 * - Datos del usuario (nombre, elo, rango, foto)
 * - Posición en ranking global
 * - Puntos por grupo muscular (para gráfico radar)
 */
class HomeViewModel : ViewModel() {

    private val _puntosRendimiento = MutableStateFlow<List<PuntosGrupoUI>>(emptyList())
    val puntosRendimiento: StateFlow<List<PuntosGrupoUI>> = _puntosRendimiento.asStateFlow()

    private val _usuarioActual = MutableStateFlow<UsuarioHome?>(null)
    val usuarioActual: StateFlow<UsuarioHome?> = _usuarioActual.asStateFlow()

    private val _posicionRanking = MutableStateFlow<Int?>(null)
    val posicionRanking: StateFlow<Int?> = _posicionRanking.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    // Límites para alcanzar Rango S por grupo muscular
    companion object {
        val LIMITES_GRUPO_MUSCULAR = mapOf(
            "Pecho" to 5000.0,
            "Espalda" to 5000.0,
            "Piernas" to 6000.0,
            "Hombros" to 4000.0,
            "Brazos" to 3500.0,
            "Core" to 3000.0
        )
    }

    private fun cargarPuntos() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            try {
                val currentUserId = AuthManager.getCurrentUserId()

                if (currentUserId == null) {
                    Log.w("HomeViewModel", "No hay usuario autenticado")
                    _error.value = "No hay sesión activa"
                    _puntosRendimiento.value = emptyList()
                    _usuarioActual.value = null
                    _posicionRanking.value = null
                    _isLoading.value = false
                    return@launch
                }

                Log.d("HomeViewModel", "Cargando datos para usuario: $currentUserId")

                val postgrestClient = SupabaseClientProvider.supabase.postgrest

                // ═══════════════════════════════════════════════════════════
                // 1. CARGAR DATOS DEL USUARIO
                // ═══════════════════════════════════════════════════════════
                val usuario = postgrestClient["usuarios"]
                    .select {
                        filter { eq("id", currentUserId) }
                    }
                    .decodeSingleOrNull<UsuarioHome>()

                _usuarioActual.value = usuario
                Log.d("HomeViewModel", "Usuario: ${usuario?.nombre}, ELO: ${usuario?.elo}, Foto: ${usuario?.fotoPerfilUrl}")

                // ═══════════════════════════════════════════════════════════
                // 2. CALCULAR POSICIÓN EN RANKING
                // ═══════════════════════════════════════════════════════════
                val todosUsuarios = postgrestClient["usuarios"]
                    .select(list("id, elo")) {
                        order("elo", Order.DESCENDING)
                    }
                    .decodeList<UsuarioEloSimple>()

                val posicion = todosUsuarios.indexOfFirst { it.id == currentUserId } + 1
                _posicionRanking.value = if (posicion > 0) posicion else null
                Log.d("HomeViewModel", "Posición en ranking: $posicion de ${todosUsuarios.size}")

                // ═══════════════════════════════════════════════════════════
                // 3. CARGAR PUNTOS POR GRUPO MUSCULAR
                // ═══════════════════════════════════════════════════════════
                try {
                    val joinColumns = "grupo, puntos_acumulados, grupo_muscular_max(puntos_max)"

                    val resultados = postgrestClient["usuario_puntos_grupo"]
                        .select(list(joinColumns)) {
                            filter {
                                eq("id_usuario", currentUserId)
                            }
                        }
                        .decodeList<PuntosAcumulados>()

                    Log.d("HomeViewModel", "Grupos musculares obtenidos: ${resultados.size}")

                    if (resultados.isNotEmpty()) {
                        val listaMapeada = resultados.map { item ->
                            PuntosGrupoUI(
                                grupo = item.grupo,
                                valor = item.puntosAcumulados,
                                maximo = item.grupoMuscularMax.puntosMax
                            )
                        }
                        _puntosRendimiento.value = completarGruposMusculares(listaMapeada)
                        Log.d("HomeViewModel", "Datos cargados de BD: ${listaMapeada.size} grupos")
                    } else {
                        // No hay datos en usuario_puntos_grupo, mostrar grupos vacíos
                        Log.w("HomeViewModel", "No hay datos en usuario_puntos_grupo, mostrando vacíos")
                        _puntosRendimiento.value = crearGruposVacios()
                    }

                } catch (e: Exception) {
                    Log.e("HomeViewModel", "Error cargando puntos grupo: ${e.message}")
                    // Si falla la carga de puntos, mostrar grupos vacíos
                    _puntosRendimiento.value = crearGruposVacios()
                }

            } catch (e: Exception) {
                Log.e("HomeViewModel", "Error al cargar datos: ${e.message}", e)
                _error.value = "Error al cargar datos: ${e.localizedMessage}"
                _puntosRendimiento.value = emptyList()
                _usuarioActual.value = null
                _posicionRanking.value = null
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Crea una lista con los 5 grupos musculares principales con valor 0
     */
    private fun crearGruposVacios(): List<PuntosGrupoUI> {
        return listOf(
            PuntosGrupoUI("Pecho", 0.0, LIMITES_GRUPO_MUSCULAR["Pecho"]!!),
            PuntosGrupoUI("Espalda", 0.0, LIMITES_GRUPO_MUSCULAR["Espalda"]!!),
            PuntosGrupoUI("Piernas", 0.0, LIMITES_GRUPO_MUSCULAR["Piernas"]!!),
            PuntosGrupoUI("Hombros", 0.0, LIMITES_GRUPO_MUSCULAR["Hombros"]!!),
            PuntosGrupoUI("Brazos", 0.0, LIMITES_GRUPO_MUSCULAR["Brazos"]!!)
        )
    }

    /**
     * Completa la lista con grupos que falten (para asegurar 5 grupos en el radar)
     */
    private fun completarGruposMusculares(lista: List<PuntosGrupoUI>): List<PuntosGrupoUI> {
        val gruposPrincipales = listOf("Pecho", "Espalda", "Piernas", "Hombros", "Brazos")
        val gruposExistentes = lista.map { it.grupo }.toSet()

        val listaCompleta = lista.toMutableList()

        gruposPrincipales.forEach { grupo ->
            if (grupo !in gruposExistentes) {
                listaCompleta.add(
                    PuntosGrupoUI(
                        grupo = grupo,
                        valor = 0.0,
                        maximo = LIMITES_GRUPO_MUSCULAR[grupo] ?: 5000.0
                    )
                )
            }
        }

        // Ordenar siempre en el mismo orden
        return listaCompleta.sortedBy { gruposPrincipales.indexOf(it.grupo) }
    }

    /**
     * Permite recargar los datos manualmente
     */
    fun recargarPuntos() {
        cargarPuntos()
    }
}

// ═══════════════════════════════════════════════════════════
// DTOs para deserialización de Supabase
// ═══════════════════════════════════════════════════════════

@Serializable
data class UsuarioHome(
    val id: String,
    val nombre: String,
    val email: String? = null,
    val altura: Int? = null,
    val peso: Int? = null,
    val elo: Int? = 0,
    val rango: String? = "Bronze",
    @SerialName("fotoperfilurl")
    val fotoPerfilUrl: String? = null
)

@Serializable
data class UsuarioEloSimple(
    val id: String,
    val elo: Int? = null
)