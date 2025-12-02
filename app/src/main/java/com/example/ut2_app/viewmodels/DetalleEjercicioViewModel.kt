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
import io.github.jan.supabase.postgrest.rpc
// 🔑 IMPORTACIONES PARA SERIALIZACIÓN SEGURA (JSON)
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.coroutines.launch
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.UUID

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
            } catch (e: Exception) {
                _error.postValue("No se pudo cargar el catálogo")
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
                _error.postValue("Error al cargar el ejercicio")
            } finally {
                _isLoading.postValue(false)
            }
        }
    }

    /**
     * Guarda el ejercicio comparando con el historial.
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
        if (nombre.isBlank() || seriesData.isEmpty()) {
            _error.value = "Datos incompletos"
            return
        }

        _isLoading.value = true
        _error.value = null

        viewModelScope.launch {
            try {
                val postgrestClient = SupabaseClientProvider.supabase.postgrest
                val currentUserId = AuthManager.getCurrentUserId() ?: return@launch

                // 1. OBTENER HISTORIAL (Semana anterior)
                // Llamada a la función RPC SQL que creamos para buscar el último peso
                val ptmAnterior: Double = try {
                    postgrestClient.rpc(
                        "obtener_ultimo_ptm_ejercicio",
                        mapOf(
                            "p_id_usuario" to currentUserId,
                            "p_id_ejercicio" to idFkEjercicio
                        )
                    ).decodeAs<Double>()
                } catch (e: Exception) {
                    Log.w("DetalleVM", "Sin historial previo o error RPC: ${e.message}")
                    0.0 // Es la primera vez
                }

                // 2. CÁLCULOS ACTUALES
                val ptmActual = PTMCalculator.calcularPTMConSeries(seriesData, dificultad)
                val usuarioActual = AuthManager.getCurrentUserData()
                val eloActualUsuario = usuarioActual?.elo ?: 1000

                // 3. COMPARACIÓN PROGRESIVA
                // Usamos la nueva función que compara Hoy vs Anterior
                val cambioELO = PTMCalculator.calcularCambioEloProgresivo(
                    ptmActual = ptmActual,
                    ptmAnterior = ptmAnterior,
                    eloActual = eloActualUsuario
                )

                val nuevoELO = PTMCalculator.aplicarCambioELO(eloActualUsuario, cambioELO)
                val nuevoRango = PTMCalculator.obtenerRango(nuevoELO)

                // PUNTOS GRUPO MUSCULAR (RADAR):
                // Según tu petición: "si ha sido igual o menor debera mantenerse"
                // Solo sumamos puntos al radar si hay MEJORA (cambioELO > 0) o es la primera vez.
                val puntosGrupoMuscular = if (cambioELO > 0 || ptmAnterior == 0.0) {
                    ptmActual * dificultad
                } else {
                    0.0 // Mantenemos (no sumamos puntos extra por no mejorar)
                }

                Log.d("DetalleVM", "Historial: $ptmAnterior | Hoy: $ptmActual | Cambio: $cambioELO")

                // 4. PREPARAR DATOS BASE DE DATOS
                val isNuevo = idEjercicio.isNullOrBlank()
                val idDatoFinal = idEjercicio ?: UUID.randomUUID().toString()
                val totalReps = seriesData.sumOf { it.second }
                val pesoMaximo = seriesData.maxOfOrNull { it.first } ?: 0.0

                // A. Insertar/Actualizar el Ejercicio en rutina_dia_datos
                val datoParaInsertar = RutinaDiaDatoInsert(
                    id_dato = idDatoFinal,
                    routine_day_id = idDiaRutina,
                    id_ejercicio = idFkEjercicio,
                    reps = totalReps,
                    peso = pesoMaximo,
                    dificultad = dificultad,
                    ptm = ptmActual,
                    elo = nuevoELO.toDouble()
                )

                if (isNuevo) {
                    postgrestClient["rutina_dia_datos"].insert(datoParaInsertar)
                } else {
                    postgrestClient["rutina_dia_datos"].update(datoParaInsertar) {
                        filter { eq("id_dato", idDatoFinal) }
                    }
                    postgrestClient["series"].delete {
                        filter { eq("id_dato", idDatoFinal) }
                    }
                }

                // B. Insertar Series
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

                // C. Actualizar Usuario (CON FIX SERIALIZACIÓN)
                val updateData = buildJsonObject {
                    put("elo", nuevoELO)
                    put("rango", nuevoRango)
                    put("ultimo_puntaje", ptmActual)
                }
                postgrestClient["usuarios"].update(updateData) {
                    filter { eq("id", currentUserId) }
                }

                // D. Actualizar Gráfico Radar (Solo si hubo mejora)
                if (puntosGrupoMuscular > 0) {
                    actualizarPuntosGrupoMuscular(currentUserId, grupoMuscular ?: "Otro", puntosGrupoMuscular)
                }

                // E. Guardar en Historial
                if (cambioELO != 0) {
                    val razonTexto = if(ptmAnterior == 0.0) "Nuevo Ejercicio"
                    else if(cambioELO > 0) "Mejora vs semana pasada"
                    else "Rendimiento inferior"

                    val historialInsert = HistorialEloInsert(
                        id = UUID.randomUUID().toString(),
                        id_usuario = currentUserId,
                        elo_anterior = eloActualUsuario.toDouble(),
                        elo_nuevo = nuevoELO.toDouble(),
                        cambio = cambioELO.toDouble(),
                        razon = razonTexto,
                        id_dato = idDatoFinal
                    )
                    postgrestClient["historial_elo"].insert(historialInsert)
                }

                _operacionExitosa.postValue("Guardado. ELO: $nuevoELO (${if(cambioELO>=0) "+" else ""}$cambioELO)")

            } catch (e: Exception) {
                Log.e("DetalleVM", "Error crítico: ${e.message}", e)
                _error.postValue("Error: ${e.localizedMessage}")
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
            null
        }
    }

    private suspend fun actualizarPuntosGrupoMuscular(
        userId: String,
        grupoMuscularInput: String,
        puntosNuevos: Double
    ) {
        try {
            // 1. Normalizar nombre (Ej: "pecho " -> "Pecho")
            val grupoMuscular = grupoMuscularInput.trim()
                .lowercase()
                .replaceFirstChar { it.uppercase() }

            val postgrestClient = SupabaseClientProvider.supabase.postgrest

            // 2. Buscar si ya existe registro para ese grupo normalizado
            val registroExistente = postgrestClient["usuario_puntos_grupo"]
                .select {
                    filter {
                        eq("id_usuario", userId)
                        eq("grupo", grupoMuscular)
                    }
                }
                .decodeSingleOrNull<PuntosGrupoExistente>()

            if (registroExistente != null) {
                // ACTUALIZAR (Sumar)
                val nuevoTotal = registroExistente.puntosAcumulados + puntosNuevos

                val updateData = buildJsonObject {
                    put("puntos_acumulados", nuevoTotal)
                }

                postgrestClient["usuario_puntos_grupo"]
                    .update(updateData) {
                        filter {
                            eq("id_usuario", userId)
                            eq("grupo", grupoMuscular)
                        }
                    }
                Log.d("DetalleVM", "Puntos actualizados para $grupoMuscular: $nuevoTotal")
            } else {
                // INSERTAR (Nuevo)
                val nuevoRegistro = PuntosGrupoInsert(
                    id_usuario = userId,
                    grupo = grupoMuscular,
                    puntos_acumulados = puntosNuevos
                )
                postgrestClient["usuario_puntos_grupo"].insert(nuevoRegistro)
                Log.d("DetalleVM", "Primer registro para $grupoMuscular: $puntosNuevos")
            }
        } catch (e: Exception) {
            Log.e("DetalleVM", "Error crítico actualizando puntos grupo: ${e.message}", e)
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