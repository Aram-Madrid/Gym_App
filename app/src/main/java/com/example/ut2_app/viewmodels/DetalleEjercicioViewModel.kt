package com.example.ut2_app.viewmodels

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ut2_app.model.Ejercicio
import com.example.ut2_app.model.EjercicioDetalle
import com.example.ut2_app.model.RutinaDiaDatoInsert
import com.example.ut2_app.model.SerieInsert
import com.example.ut2_app.util.AuthManager
import com.example.ut2_app.util.PTMCalculator
import com.example.ut2_app.util.SupabaseClientProvider
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns.Companion.list
import kotlinx.coroutines.launch
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.UUID

/**
 * ViewModel para DetalleEjercicioActivity.
 *
 * Al guardar un ejercicio, actualiza:
 * 1. rutina_dia_datos - datos del ejercicio
 * 2. series - series individuales
 * 3. usuarios.elo - ELO general (para ranking)
 * 4. usuario_puntos_grupo - puntos por grupo muscular (para gráfico radar)
 * 5. historial_elo - registro del cambio
 */
class DetalleEjercicioViewModel : ViewModel() {

    private val _catalogoEjercicios = MutableLiveData<List<EjercicioDetalle>>(emptyList())
    val catalogoEjercicios: LiveData<List<EjercicioDetalle>> = _catalogoEjercicios

    private val _ejercicio = MutableLiveData<Ejercicio?>(null)
    val ejercicio: LiveData<Ejercicio?> = _ejercicio

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _operacionExitosa = MutableLiveData<String?>(null)
    val operacionExitosa: LiveData<String?> = _operacionExitosa

    private val _error = MutableLiveData<String?>(null)
    val error: LiveData<String?> = _error

    init {
        cargarCatalogo()
    }

    fun cargarCatalogo() {
        viewModelScope.launch {
            try {
                _isLoading.postValue(true)
                val postgrestClient = SupabaseClientProvider.supabase.postgrest
                val result = postgrestClient["ejercicio"]
                    .select(list("id_ejercicio, nombre, grupo_muscular, dificultad"))
                    .decodeList<EjercicioDetalle>()
                _catalogoEjercicios.postValue(result)
                Log.d("DetalleEjercicioVM", "Catálogo cargado: ${result.size} ejercicios")
            } catch (e: Exception) {
                Log.e("DetalleEjercicioVM", "Error cargando catálogo: ${e.message}", e)
                _error.postValue("No se pudo cargar el catálogo de ejercicios")
            } finally {
                _isLoading.postValue(false)
            }
        }
    }

    fun cargarEjercicio(idEjercicio: String) {
        viewModelScope.launch {
            _isLoading.postValue(true)
            try {
                val postgrestClient = SupabaseClientProvider.supabase.postgrest
                val result = postgrestClient["rutina_dia_datos"]
                    .select {
                        filter { eq("id_dato", idEjercicio) }
                    }
                    .decodeSingleOrNull<Ejercicio>()
                _ejercicio.postValue(result)
            } catch (e: Exception) {
                Log.e("DetalleEjercicioVM", "Error cargando ejercicio: ${e.message}", e)
                _error.postValue("Error al cargar el ejercicio")
            } finally {
                _isLoading.postValue(false)
            }
        }
    }

    /**
     * Guarda el ejercicio y actualiza todos los sistemas de puntuación.
     */
    fun guardarEjercicioConSeries(
        idEjercicio: String?,
        idDiaRutina: String,
        nombre: String,
        seriesData: List<Pair<Double, Int>>,
        dificultad: Double,
        idFkEjercicio: String,
        grupoMuscular: String? = null
    ) {
        if (nombre.isBlank()) {
            _error.value = "El nombre es obligatorio."
            return
        }

        if (seriesData.isEmpty()) {
            _error.value = "Debe registrar al menos 1 serie."
            return
        }

        _isLoading.value = true
        _error.value = null

        viewModelScope.launch {
            try {
                val postgrestClient = SupabaseClientProvider.supabase.postgrest

                val isNuevo = idEjercicio.isNullOrBlank()
                val idDatoFinal = idEjercicio ?: UUID.randomUUID().toString()

                // Obtener datos del catálogo si no se proporcionaron
                val ejercicioCatalogo = obtenerEjercicioCatalogo(idFkEjercicio)
                val grupoMuscularFinal = grupoMuscular
                    ?: ejercicioCatalogo?.grupoMuscular
                    ?: "Otro"
                val dificultadFinal = ejercicioCatalogo?.dificultad ?: dificultad

                // Calcular totales
                val totalReps = seriesData.sumOf { it.second }
                val pesoMaximo = seriesData.maxOfOrNull { it.first } ?: 0.0

                // Calcular PTM
                val ptm = PTMCalculator.calcularPTMConSeries(seriesData, dificultadFinal)

                // Obtener usuario actual
                val currentUserId = AuthManager.getCurrentUserId()
                if (currentUserId == null) {
                    _error.postValue("No hay sesión activa")
                    return@launch
                }

                val usuarioActual = AuthManager.getCurrentUserData()
                val eloActualUsuario = usuarioActual?.elo ?: 1000

                // Calcular cambio de ELO
                val cambioELO = PTMCalculator.calcularCambioELO(ptm, eloActualUsuario)
                val nuevoELO = PTMCalculator.aplicarCambioELO(eloActualUsuario, cambioELO)

                // Calcular puntos para grupo muscular
                val puntosGrupoMuscular = ptm * dificultadFinal

                Log.d("DetalleEjercicioVM", "════════════════════════════════════════")
                Log.d("DetalleEjercicioVM", "GUARDANDO EJERCICIO")
                Log.d("DetalleEjercicioVM", "────────────────────────────────────────")
                Log.d("DetalleEjercicioVM", "Nombre: $nombre")
                Log.d("DetalleEjercicioVM", "Grupo Muscular: $grupoMuscularFinal")
                Log.d("DetalleEjercicioVM", "Dificultad: $dificultadFinal")
                Log.d("DetalleEjercicioVM", "Series: ${seriesData.size}")
                Log.d("DetalleEjercicioVM", "PTM: $ptm")
                Log.d("DetalleEjercicioVM", "ELO: $eloActualUsuario → $nuevoELO (${if (cambioELO >= 0) "+$cambioELO" else cambioELO})")
                Log.d("DetalleEjercicioVM", "Puntos $grupoMuscularFinal: +$puntosGrupoMuscular")
                Log.d("DetalleEjercicioVM", "════════════════════════════════════════")

                // ═══════════════════════════════════════════════════════════
                // 1. GUARDAR EJERCICIO EN rutina_dia_datos
                // ═══════════════════════════════════════════════════════════
                val datoParaInsertar = RutinaDiaDatoInsert(
                    id_dato = idDatoFinal,
                    routine_day_id = idDiaRutina,
                    id_ejercicio = idFkEjercicio,
                    reps = totalReps,
                    peso = pesoMaximo,
                    dificultad = dificultadFinal,
                    ptm = ptm,
                    elo = nuevoELO.toDouble()
                )

                if (isNuevo) {
                    postgrestClient["rutina_dia_datos"].insert(datoParaInsertar)
                } else {
                    postgrestClient["rutina_dia_datos"]
                        .update(datoParaInsertar) {
                            filter { eq("id_dato", idDatoFinal) }
                        }
                    postgrestClient["series"]
                        .delete {
                            filter { eq("id_dato", idDatoFinal) }
                        }
                }
                Log.d("DetalleEjercicioVM", "✅ Ejercicio guardado")

                // ═══════════════════════════════════════════════════════════
                // 2. GUARDAR SERIES
                // ═══════════════════════════════════════════════════════════
                seriesData.forEachIndexed { index, (peso, reps) ->
                    val serieInsert = SerieInsert(
                        id_serie = UUID.randomUUID().toString(),
                        id_dato = idDatoFinal,
                        numero_serie = index + 1,
                        peso = peso,
                        repeticiones = reps
                    )
                    postgrestClient["series"].insert(serieInsert)
                }
                Log.d("DetalleEjercicioVM", "✅ ${seriesData.size} series guardadas")

                // ═══════════════════════════════════════════════════════════
                // 3. ACTUALIZAR ELO DEL USUARIO (RANKING)
                // ═══════════════════════════════════════════════════════════
                val nuevoRango = PTMCalculator.obtenerRango(nuevoELO)

                postgrestClient["usuarios"]
                    .update(mapOf(
                        "elo" to nuevoELO,
                        "rango" to nuevoRango,
                        "ultimo_puntaje" to ptm
                    )) {
                        filter { eq("id", currentUserId) }
                    }
                Log.d("DetalleEjercicioVM", "✅ ELO actualizado: $nuevoELO ($nuevoRango)")

                // ═══════════════════════════════════════════════════════════
                // 4. ACTUALIZAR PUNTOS POR GRUPO MUSCULAR (GRÁFICO RADAR)
                // ═══════════════════════════════════════════════════════════
                actualizarPuntosGrupoMuscular(currentUserId, grupoMuscularFinal, puntosGrupoMuscular)

                // ═══════════════════════════════════════════════════════════
                // 5. GUARDAR HISTORIAL DE ELO
                // ═══════════════════════════════════════════════════════════
                if (cambioELO != 0) {
                    try {
                        val historialInsert = HistorialEloInsert(
                            id = UUID.randomUUID().toString(),
                            id_usuario = currentUserId,
                            elo_anterior = eloActualUsuario.toDouble(),
                            elo_nuevo = nuevoELO.toDouble(),
                            cambio = cambioELO.toDouble(),
                            razon = "Ejercicio: $nombre",
                            id_dato = idDatoFinal
                        )
                        postgrestClient["historial_elo"].insert(historialInsert)
                        Log.d("DetalleEjercicioVM", "✅ Historial guardado")
                    } catch (e: Exception) {
                        Log.e("DetalleEjercicioVM", "⚠️ Error guardando historial: ${e.message}")
                    }
                }

                _operacionExitosa.postValue(
                    "¡Guardado! ELO ${if (cambioELO >= 0) "+$cambioELO" else cambioELO} | $grupoMuscularFinal +${puntosGrupoMuscular.toInt()} pts"
                )

            } catch (e: Exception) {
                Log.e("DetalleEjercicioVM", "❌ Error al guardar: ${e.message}", e)
                _error.postValue("Error al guardar: ${e.localizedMessage ?: e.message}")
            } finally {
                _isLoading.postValue(false)
            }
        }
    }

    private suspend fun obtenerEjercicioCatalogo(idEjercicio: String): EjercicioCatalogoData? {
        return try {
            val postgrestClient = SupabaseClientProvider.supabase.postgrest
            postgrestClient["ejercicio"]
                .select(list("id_ejercicio, nombre, grupo_muscular, dificultad")) {
                    filter { eq("id_ejercicio", idEjercicio) }
                }
                .decodeSingleOrNull<EjercicioCatalogoData>()
        } catch (e: Exception) {
            Log.e("DetalleEjercicioVM", "Error obteniendo catálogo: ${e.message}")
            null
        }
    }

    private suspend fun actualizarPuntosGrupoMuscular(
        userId: String,
        grupoMuscular: String,
        puntosNuevos: Double
    ) {
        try {
            val postgrestClient = SupabaseClientProvider.supabase.postgrest

            // Verificar si existe el registro
            val registroExistente = postgrestClient["usuario_puntos_grupo"]
                .select {
                    filter {
                        eq("id_usuario", userId)
                        eq("grupo", grupoMuscular)
                    }
                }
                .decodeSingleOrNull<PuntosGrupoExistente>()

            if (registroExistente != null) {
                // Actualizar: sumar puntos
                val nuevoTotal = registroExistente.puntosAcumulados + puntosNuevos

                postgrestClient["usuario_puntos_grupo"]
                    .update(mapOf("puntos_acumulados" to nuevoTotal)) {
                        filter {
                            eq("id_usuario", userId)
                            eq("grupo", grupoMuscular)
                        }
                    }
                Log.d("DetalleEjercicioVM", "✅ $grupoMuscular: ${registroExistente.puntosAcumulados} + $puntosNuevos = $nuevoTotal")
            } else {
                // Insertar nuevo registro
                val nuevoRegistro = PuntosGrupoInsert(
                    id_usuario = userId,
                    grupo = grupoMuscular,
                    puntos_acumulados = puntosNuevos
                )
                postgrestClient["usuario_puntos_grupo"].insert(nuevoRegistro)
                Log.d("DetalleEjercicioVM", "✅ $grupoMuscular: nuevo registro con $puntosNuevos pts")
            }

        } catch (e: Exception) {
            Log.e("DetalleEjercicioVM", "⚠️ Error actualizando puntos grupo: ${e.message}", e)
        }
    }

    fun clearOperationStatus() {
        _operacionExitosa.value = null
        _error.value = null
    }
}

// ═══════════════════════════════════════════════════════════
// DTOs
// ═══════════════════════════════════════════════════════════

@Serializable
data class EjercicioCatalogoData(
    @SerialName("id_ejercicio")
    val idEjercicio: String,
    val nombre: String,
    @SerialName("grupo_muscular")
    val grupoMuscular: String? = null,
    val dificultad: Double? = null
)

@Serializable
data class PuntosGrupoExistente(
    @SerialName("id_usuario")
    val idUsuario: String,
    val grupo: String,
    @SerialName("puntos_acumulados")
    val puntosAcumulados: Double
)

@Serializable
data class PuntosGrupoInsert(
    val id_usuario: String,
    val grupo: String,
    val puntos_acumulados: Double
)

@Serializable
data class HistorialEloInsert(
    val id: String,
    val id_usuario: String,
    val elo_anterior: Double,
    val elo_nuevo: Double,
    val cambio: Double,
    val razon: String?,
    val id_dato: String?
)