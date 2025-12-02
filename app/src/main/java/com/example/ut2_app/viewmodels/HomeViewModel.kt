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

    // Inicializamos cargando datos si quieres, o esperas al fragmento
    // init { cargarPuntos() }

    private fun cargarPuntos() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            try {
                val currentUserId = AuthManager.getCurrentUserId()

                if (currentUserId == null) {
                    _puntosRendimiento.value = crearGruposVacios()
                    _usuarioActual.value = null
                    _isLoading.value = false
                    return@launch
                }

                val postgrestClient = SupabaseClientProvider.supabase.postgrest

                // 1. CARGAR USUARIO
                val usuario = postgrestClient["usuarios"]
                    .select { filter { eq("id", currentUserId) } }
                    .decodeSingleOrNull<UsuarioHome>()
                _usuarioActual.value = usuario

                // 2. RANKING
                val amigosResponse = postgrestClient["amigos"]
                    .select { filter { eq("id", currentUserId) } }
                    .decodeList<Map<String, String>>()

                val friendIds = amigosResponse.mapNotNull { it["id_amigo"] }.toMutableList()
                friendIds.add(currentUserId) // CRÍTICO: Incluir al usuario actual

                // 2b. Obtener SOLO la lista de usuarios (Amigos + Tú) ORDENADOS por ELO
                val rankingAmigos = postgrestClient["usuarios"]
                    .select(list("id, elo")) {
                        filter { isIn("id", friendIds) } // Filtra por la lista de IDs
                        order("elo", Order.DESCENDING) // Ordenar por ELO
                    }
                    .decodeList<UsuarioEloSimple>()

                // 2c. Calcular la posición (índice + 1) en la lista filtrada
                val posicion = rankingAmigos.indexOfFirst { it.id == currentUserId } + 1
                _posicionRanking.value = if (posicion > 0) posicion else null

                // 3. PUNTOS GRUPO (Con normalización)
                try {
                    val resultados = postgrestClient["usuario_puntos_grupo"]
                        .select(list("grupo, puntos_acumulados, grupo_muscular_max(puntos_max)")) {
                            filter { eq("id_usuario", currentUserId) }
                        }
                        .decodeList<PuntosAcumulados>()

                    if (resultados.isNotEmpty()) {
                        val gruposNormalizados = resultados.groupBy {
                            it.grupo.trim().replaceFirstChar { c -> c.uppercase() }
                        }.map { (nombre, lista) ->
                            PuntosGrupoUI(
                                grupo = nombre,
                                valor = lista.sumOf { it.puntosAcumulados }, // Sumar si hay duplicados
                                maximo = lista.first().grupoMuscularMax.puntosMax
                            )
                        }

                        _puntosRendimiento.value = completarGruposMusculares(gruposNormalizados)
                    } else {
                        _puntosRendimiento.value = crearGruposVacios()
                    }

                } catch (e: Exception) {
                    Log.e("HomeVM", "Error puntos: ${e.message}")
                    _puntosRendimiento.value = crearGruposVacios()
                }

            } catch (e: Exception) {
                _error.value = "Error: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun crearGruposVacios(): List<PuntosGrupoUI> {
        return LIMITES_GRUPO_MUSCULAR.map { (nombre, max) ->
            PuntosGrupoUI(nombre, 0.0, max)
        }.sortedBy { listOf("Pecho", "Espalda", "Piernas", "Hombros", "Brazos", "Core").indexOf(it.grupo) }
    }

    private fun completarGruposMusculares(lista: List<PuntosGrupoUI>): List<PuntosGrupoUI> {
        val gruposPrincipales = listOf("Pecho", "Espalda", "Piernas", "Hombros", "Brazos")

        val listaUnica = lista.distinctBy { it.grupo }
        val gruposExistentes = listaUnica.map { it.grupo }.toSet()

        val listaCompleta = listaUnica.toMutableList()

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

        // Orden fijo para que el gráfico no "baile"
        return listaCompleta.sortedBy { gruposPrincipales.indexOf(it.grupo) }
    }

    fun recargarPuntos() {
        cargarPuntos()
    }
}

// DTOs (Se mantienen)
@Serializable
data class UsuarioHome(
    val id: String,
    val nombre: String,
    val email: String? = null,
    val altura: Int? = null,
    val peso: Int? = null,
    val elo: Int? = 0,
    val rango: String? = "Bronze",
    @SerialName("fotoperfilurl") val fotoPerfilUrl: String? = null
)

@Serializable
data class UsuarioEloSimple(val id: String, val elo: Int? = null)