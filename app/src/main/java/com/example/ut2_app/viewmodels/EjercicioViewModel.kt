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
            // 🔑 MANEJO CORRECTO DE NULL: Si no hay ID, lista vacía
            if (idDiaRutina == null) {
                Log.d("EjercicioViewModel", "Modo creación: sin ID de día, mostrando lista vacía")
                _listaEjercicios.postValue(emptyList())
                return@launch
            }

            _isLoading.postValue(true)
            _error.postValue(null)

            try {
                val postgrestClient = SupabaseClientProvider.supabase.postgrest

                // 🔑 AHORA idDiaRutina es String (no null), seguro para usar
                val resultados = postgrestClient["rutina_dia_datos"]
                    .select(list("*, ejercicio(*)")) {
                        filter {
                            eq("routine_day_id", idDiaRutina)
                        }
                    }
                    .decodeList<RutinaDiaDatosConEjercicio>()

                // 🔑 Para cada ejercicio, cargar sus series individuales
                val listaMapeada = resultados.map { item ->
                    // Cargar series de este ejercicio
                    val seriesDelEjercicio = try {
                        postgrestClient["series"]
                            .select {
                                filter {
                                    eq("id_dato", item.id_dato)
                                }
                            }
                            .decodeList<SerieDB>()
                            .sortedBy { it.numeroSerie } // Ordenar por número de serie
                            .map { serieDB ->
                                Serie(
                                    peso = serieDB.peso,
                                    repeticiones = serieDB.repeticiones
                                )
                            }
                    } catch (e: Exception) {
                        Log.e("EjercicioViewModel", "Error cargando series: ${e.message}")
                        emptyList()
                    }

                    Ejercicio(
                        idDato = item.id_dato,
                        nombre = item.ejercicio.nombre,
                        reps = item.reps,
                        peso = item.peso,
                        dificultad = item.dificultad,
                        series = seriesDelEjercicio // 🔑 Ahora incluye las series reales
                    )
                }

                _listaEjercicios.postValue(listaMapeada)
                Log.d("EjercicioViewModel", "Cargados ${listaMapeada.size} ejercicios para día: $idDiaRutina")

            } catch (e: Exception) {
                Log.e("EjercicioViewModel", "Error al cargar ejercicios: ${e.message}", e)
                _error.postValue("Error al cargar ejercicios: ${e.localizedMessage}")
                _listaEjercicios.postValue(emptyList())
            } finally {
                _isLoading.postValue(false)
            }
        }
    }

    /**
     * Guarda un ejercicio nuevo (versión legacy - considera usar guardarEjercicioConSeries).
     * Esta función se mantiene por compatibilidad pero no guarda series individuales.
     */
    suspend fun guardarEjercicio(ejercicio: Ejercicio) {
        // 🔑 Validar que tengamos un ID de día válido
        val idDia = idDiaRutina
            ?: throw IllegalStateException("No se puede guardar ejercicio sin ID de día activo.")

        val postgrestClient = SupabaseClientProvider.supabase.postgrest

        // Generar nuevo ID para el registro
        val idDatoFinal = UUID.randomUUID().toString()

        // El idDato del ejercicio es en realidad el id_ejercicio (FK al catálogo)
        val idFkEjercicio = ejercicio.idDato

        // 🔑 CALCULAR PTM Y ELO
        val ptm = PTMCalculator.calcularPTM(ejercicio.peso, ejercicio.reps, ejercicio.dificultad)

        // Obtener ELO actual del usuario para calcular cambio
        val usuarioActual = AuthManager.getCurrentUserData()
        val eloActualUsuario = usuarioActual?.elo ?: 1000

        val cambioELO = PTMCalculator.calcularCambioELO(ptm, eloActualUsuario)
        val nuevoELO = PTMCalculator.aplicarCambioELO(eloActualUsuario, cambioELO)

        Log.d("EjercicioViewModel", "Guardando ejercicio: PTM=$ptm, Cambio ELO=$cambioELO")

        // 🔑 USAR NOMBRE CORRECTO: routine_day_id
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
            Log.d("EjercicioViewModel", "Ejercicio guardado exitosamente: $idDatoFinal")
        } catch (e: Exception) {
            Log.e("EjercicioViewModel", "Fallo al insertar ejercicio: ${e.message}", e)
            throw e
        }
    }
}