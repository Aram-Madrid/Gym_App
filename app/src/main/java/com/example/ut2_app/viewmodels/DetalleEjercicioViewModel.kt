package com.example.ut2_app.viewmodels

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ut2_app.model.Ejercicio
import com.example.ut2_app.model.EjercicioDetalle
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
                val postgrestClient = SupabaseClientProvider.supabase.postgrest
                val result = postgrestClient["ejercicio"]
                    .select(list("id_ejercicio, nombre, grupo_muscular"))
                    .decodeList<EjercicioDetalle>()
                _catalogoEjercicios.postValue(result)
            } catch (e: Exception) {
                Log.e("DetalleEjercicioVM", "Error cargando catálogo: ${e.message}")
            }
        }
    }

    fun cargarEjercicio(idEjercicio: String) {
        _isLoading.value = true
        _error.value = null
        viewModelScope.launch {
            try {
                val postgrestClient = SupabaseClientProvider.supabase.postgrest
                val result = postgrestClient["rutina_dia_datos"]
                    .select {
                        filter { eq("id_dato", idEjercicio) }
                    }
                    .decodeSingleOrNull<Ejercicio>()
                _ejercicio.postValue(result)
            } catch (e: Exception) {
                Log.e("DetalleEjercicioVM", "Error al obtener ejercicio: ${e.message}", e)
                _error.postValue("Error al obtener ejercicio: ${e.localizedMessage ?: e.message}")
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
        dificultad: Double
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

                val nuevoDatoMap = mapOf(
                    "id_dato" to idDatoFinal,
                    "id_dia" to idDiaRutina,
                    "id_ejercicio" to idDatoFinal,
                    "reps" to reps,
                    "peso" to peso,
                    "dificultad" to dificultad
                )

                if (isNuevo) {
                    SupabaseClientProvider.supabase.postgrest["rutina_dia_datos"].insert(nuevoDatoMap)
                    _operacionExitosa.postValue("Ejercicio creado con éxito.")
                } else {
                    SupabaseClientProvider.supabase.postgrest["rutina_dia_datos"]
                        .update(nuevoDatoMap) {
                            filter { eq("id_dato", idDatoFinal) }
                        }
                    _operacionExitosa.postValue("Ejercicio actualizado con éxito.")
                }

            } catch (e: Exception) {
                Log.e("DetalleEjercicioVM", "Error al guardar ejercicio: ${e.message}", e)
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