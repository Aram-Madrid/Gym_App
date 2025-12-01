package com.example.ut2_app.viewmodels

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ut2_app.model.Ejercicio
import com.example.ut2_app.model.EjercicioDetalle
import com.example.ut2_app.model.RutinaDiaDatoInsert
import com.example.ut2_app.util.SupabaseClientProvider
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns.Companion.list
import kotlinx.coroutines.launch
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
                    .select(list("id_ejercicio, nombre, grupo_muscular"))
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

    fun guardarEjercicio(
        idEjercicio: String?,
        idDiaRutina: String,
        nombre: String,
        reps: Int,
        peso: Double,
        dificultad: Double,
        idFkEjercicio: String
    ) {
        if (nombre.isBlank()) {
            _error.value = "El nombre es obligatorio."
            return
        }

        _isLoading.value = true
        _error.value = null

        viewModelScope.launch {
            try {
                val isNuevo = idEjercicio.isNullOrBlank()
                val idDatoFinal = idEjercicio ?: UUID.randomUUID().toString()

                Log.d("DetalleEjercicioVM", "=== GUARDANDO EJERCICIO ===")
                Log.d("DetalleEjercicioVM", "ID Dato: $idDatoFinal")
                Log.d("DetalleEjercicioVM", "ID Día Rutina: $idDiaRutina")
                Log.d("DetalleEjercicioVM", "ID Ejercicio (FK): $idFkEjercicio")
                Log.d("DetalleEjercicioVM", "Nombre: $nombre")
                Log.d("DetalleEjercicioVM", "Reps: $reps")
                Log.d("DetalleEjercicioVM", "Peso: $peso")
                Log.d("DetalleEjercicioVM", "Dificultad: $dificultad")
                Log.d("DetalleEjercicioVM", "Es Nuevo: $isNuevo")

                // 🔑 CORRECCIÓN: Usar 'routine_day_id' en lugar de 'id_dia'
                val datoParaInsertar = RutinaDiaDatoInsert(
                    id_dato = idDatoFinal,
                    routine_day_id = idDiaRutina,
                    id_ejercicio = idFkEjercicio,
                    reps = reps,
                    peso = peso,
                    dificultad = dificultad
                )

                if (isNuevo) {
                    SupabaseClientProvider.supabase.postgrest["rutina_dia_datos"]
                        .insert(datoParaInsertar)
                    _operacionExitosa.postValue("Ejercicio creado con éxito.")
                    Log.d("DetalleEjercicioVM", "✅ Ejercicio insertado exitosamente: $idDatoFinal")
                } else {
                    SupabaseClientProvider.supabase.postgrest["rutina_dia_datos"]
                        .update(datoParaInsertar) {
                            filter { eq("id_dato", idDatoFinal) }
                        }
                    _operacionExitosa.postValue("Ejercicio actualizado con éxito.")
                    Log.d("DetalleEjercicioVM", "✅ Ejercicio actualizado exitosamente: $idDatoFinal")
                }

            } catch (e: Exception) {
                Log.e("DetalleEjercicioVM", "❌ Error al guardar ejercicio: ${e.message}", e)
                _error.postValue("Error al guardar: ${e.localizedMessage ?: e.message}")
            } finally {
                _isLoading.postValue(false)
            }
        }
    }

    fun clearOperationStatus() {
        _operacionExitosa.value = null
        _error.value = null
    }
}