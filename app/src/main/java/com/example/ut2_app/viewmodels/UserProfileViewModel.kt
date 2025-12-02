package com.example.ut2_app.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ut2_app.model.PuntosAcumulados
import com.example.ut2_app.model.PuntosGrupoUI
import com.example.ut2_app.util.SupabaseClientProvider
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns.Companion.list
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class UserProfileViewModel : ViewModel() {

    private val _puntosRendimiento = MutableStateFlow<List<PuntosGrupoUI>>(emptyList())
    val puntosRendimiento: StateFlow<List<PuntosGrupoUI>> = _puntosRendimiento.asStateFlow()

    private val _usuarioSeleccionado = MutableStateFlow<UsuarioHome?>(null)
    val usuarioSeleccionado: StateFlow<UsuarioHome?> = _usuarioSeleccionado.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    fun cargarPerfil(userId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val postgrestClient = SupabaseClientProvider.supabase.postgrest

                // 1. Cargar datos básicos del usuario (Nombre, ELO, Rango, Foto)
                val usuario = postgrestClient["usuarios"]
                    .select { filter { eq("id", userId) } }
                    .decodeSingleOrNull<UsuarioHome>()
                _usuarioSeleccionado.value = usuario

                // 2. Cargar puntos del gráfico de radar
                try {
                    val resultados = postgrestClient["usuario_puntos_grupo"]
                        .select(list("grupo, puntos_acumulados, grupo_muscular_max(puntos_max)")) {
                            filter { eq("id_usuario", userId) }
                        }
                        .decodeList<PuntosAcumulados>()

                    if (resultados.isNotEmpty()) {
                        val gruposNormalizados = resultados.groupBy {
                            it.grupo.trim().replaceFirstChar { c -> c.uppercase() }
                        }.map { (nombre, lista) ->
                            PuntosGrupoUI(
                                grupo = nombre,
                                valor = lista.sumOf { it.puntosAcumulados },
                                maximo = lista.first().grupoMuscularMax.puntosMax
                            )
                        }
                        _puntosRendimiento.value = completarGruposMusculares(gruposNormalizados)
                    } else {
                        _puntosRendimiento.value = crearGruposVacios()
                    }
                } catch (e: Exception) {
                    _puntosRendimiento.value = crearGruposVacios()
                }

            } catch (e: Exception) {
                Log.e("UserProfileVM", "Error al cargar perfil: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    // Reutilizamos la lógica de HomeViewModel para rellenar huecos
    private fun crearGruposVacios(): List<PuntosGrupoUI> {
        return HomeViewModel.LIMITES_GRUPO_MUSCULAR.map { (nombre, max) ->
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
                    PuntosGrupoUI(grupo, 0.0, HomeViewModel.LIMITES_GRUPO_MUSCULAR[grupo] ?: 5000.0)
                )
            }
        }
        return listaCompleta.sortedBy { gruposPrincipales.indexOf(it.grupo) }
    }
}