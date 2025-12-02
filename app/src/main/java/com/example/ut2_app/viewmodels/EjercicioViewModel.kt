package com.example.ut2_app.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ut2_app.model.Ejercicio
import com.example.ut2_app.model.RutinaDiaDatosConEjercicio
import com.example.ut2_app.model.RutinaDiaDatoInsert
import com.example.ut2_app.model.Serie
import com.example.ut2_app.model.SerieDB
import com.example.ut2_app.util.AuthManager
import com.example.ut2_app.util.PTMCalculator
import com.example.ut2_app.util.SupabaseClientProvider
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.launch
import android.util.Log
import io.github.jan.supabase.postgrest.query.Columns.Companion.list
// 🔑 Importaciones para la actualización manual segura
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.util.UUID

class EjercicioViewModel(private val idDiaRutina: String?) : ViewModel() {

    private val _listaEjercicios = MutableLiveData<List<Ejercicio>>()
    val listaEjercicios: LiveData<List<Ejercicio>> = _listaEjercicios

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>(null)
    val error: LiveData<String?> = _error

    init {
        cargarEjercicios()
    }

    fun cargarEjercicios() {
        viewModelScope.launch {
            if (idDiaRutina == null) {
                _listaEjercicios.postValue(emptyList())
                return@launch
            }

            _isLoading.postValue(true)
            _error.postValue(null)

            try {
                val postgrestClient = SupabaseClientProvider.supabase.postgrest

                val resultados = postgrestClient["rutina_dia_datos"]
                    .select(list("*, ejercicio(*)")) {
                        filter { eq("routine_day_id", idDiaRutina) }
                    }
                    .decodeList<RutinaDiaDatosConEjercicio>()

                val listaMapeada = resultados.map { item ->
                    val seriesDelEjercicio = try {
                        postgrestClient["series"]
                            .select { filter { eq("id_dato", item.id_dato) } }
                            .decodeList<SerieDB>()
                            .sortedBy { it.numeroSerie }
                            .map { serieDB ->
                                Serie(serieDB.peso, serieDB.repeticiones)
                            }
                    } catch (e: Exception) { emptyList() }

                    Ejercicio(
                        idDato = item.id_dato,
                        nombre = item.ejercicio.nombre,
                        reps = item.reps,
                        peso = item.peso,
                        dificultad = item.dificultad,
                        series = seriesDelEjercicio
                    )
                }

                _listaEjercicios.postValue(listaMapeada)

            } catch (e: Exception) {
                _error.postValue("Error al cargar: ${e.message}")
                _listaEjercicios.postValue(emptyList())
            } finally {
                _isLoading.postValue(false)
            }
        }
    }

    /**
     * Guarda ejercicio (Legacy/Rápido).
     * 🔑 ACTUALIZADO: Ahora actualiza manualmente el ELO del usuario porque ya no usamos triggers.
     */
    suspend fun guardarEjercicio(ejercicio: Ejercicio) {
        val idDia = idDiaRutina ?: throw IllegalStateException("Sin ID de día.")
        val postgrestClient = SupabaseClientProvider.supabase.postgrest
        val idDatoFinal = UUID.randomUUID().toString()
        val idFkEjercicio = ejercicio.idDato

        // 1. Calcular PTM y ELO (Usando función compatible)
        val ptm = PTMCalculator.calcularPTM(ejercicio.peso, ejercicio.reps, ejercicio.dificultad)

        // Obtener usuario actual
        val currentUserId = AuthManager.getCurrentUserId() ?: return
        val usuarioActual = AuthManager.getCurrentUserData()
        val eloActualUsuario = usuarioActual?.elo ?: 1000

        // Calculamos cambio (Asumimos 0.0 de historial para esta inserción rápida)
        val cambioELO = PTMCalculator.calcularCambioELO(ptm, eloActualUsuario)
        val nuevoELO = PTMCalculator.aplicarCambioELO(eloActualUsuario, cambioELO)
        val nuevoRango = PTMCalculator.obtenerRango(nuevoELO)

        Log.d("EjercicioViewModel", "Guardando legacy: PTM=$ptm, NuevoELO=$nuevoELO")

        // 2. Insertar Dato
        val datoParaInsertar = RutinaDiaDatoInsert(
            id_dato = idDatoFinal,
            routine_day_id = idDia,
            id_ejercicio = idFkEjercicio,
            reps = ejercicio.reps,
            peso = ejercicio.peso,
            dificultad = ejercicio.dificultad,
            ptm = ptm,
            elo = nuevoELO.toDouble()
        )

        try {
            postgrestClient["rutina_dia_datos"].insert(datoParaInsertar)

            // 3. 🔑 ACTUALIZAR USUARIO MANUALMENTE (Reemplazo del trigger borrado)
            val updateData = buildJsonObject {
                put("elo", nuevoELO)
                put("rango", nuevoRango)
                put("ultimo_puntaje", ptm)
            }
            postgrestClient["usuarios"].update(updateData) {
                filter { eq("id", currentUserId) }
            }

            Log.d("EjercicioViewModel", "Guardado completo (Datos + Usuario actualizado)")

        } catch (e: Exception) {
            Log.e("EjercicioViewModel", "Fallo al insertar: ${e.message}", e)
            throw e
        }
    }
}