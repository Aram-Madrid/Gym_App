package com.example.ut2_app.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ut2_app.model.Ejercicio
import com.example.ut2_app.model.RutinaDiaDatosConEjercicio
import com.example.ut2_app.util.SupabaseClientProvider
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.launch
import android.util.Log // Asegúrate de importar Log
import io.github.jan.supabase.postgrest.query.Columns.Companion.list

class EjercicioViewModel(private val idDia: String) : ViewModel() {

    private val _listaEjercicios = MutableLiveData<List<Ejercicio>>()
    val listaEjercicios: LiveData<List<Ejercicio>> = _listaEjercicios

    init {
        // Cargar inmediatamente al inicializar
        cargarEjercicios()
    }

    // 🔑 FUNCIÓN CORREGIDA: No necesita argumentos, usa la propiedad interna idDia
    fun cargarEjercicios() {
        viewModelScope.launch {
            try {
                val postgrestClient = SupabaseClientProvider.supabase.postgrest

                // Pedimos *, ejercicio(*) para el JOIN
                val resultados = postgrestClient["rutina_dia_datos"]
                    .select(list("*, ejercicio(*)") /* ⬅️ Sintaxis estricta */) {
                        // 🔑 Filtramos usando la propiedad interna
                        filter { eq("id_dia", idDia) }
                    }
                    .decodeList<RutinaDiaDatosConEjercicio>()

                val listaMapeada = resultados.map { item ->
                    // Mapeo asegurado para Ejercicio (Modelo UI)
                    Ejercicio(
                        idDato = item.id_dato,
                        nombre = item.ejercicio.nombre,
                        reps = item.reps,
                        peso = item.peso,
                        dificultad = item.dificultad
                        // series = emptyList() // Asumimos que series tiene valor por defecto
                    )
                }

                _listaEjercicios.postValue(listaMapeada)

            } catch (e: Exception) {
                Log.e("EjercicioViewModel", "Error al cargar ejercicios: ${e.message}", e)
                _listaEjercicios.postValue(emptyList())
            }
        }
    }

    // 🔑 FUNCIÓN CORREGIDA: Ya no necesita recibir idDia como argumento
    suspend fun guardarEjercicio(ejercicio: Ejercicio) {
        val postgrestClient = SupabaseClientProvider.supabase.postgrest

        // 🔑 Mapeamos los datos para la inserción
        val nuevoDato = mapOf(
            "id_dia" to idDia, // ⬅️ Usa la propiedad interna (this.idDia)
            "id_ejercicio" to ejercicio.idDato,
            "reps" to ejercicio.reps,
            "peso" to ejercicio.peso,
            "dificultad" to ejercicio.dificultad
        )

        try {
            postgrestClient["rutina_dia_datos"].insert(nuevoDato)
        } catch (e: Exception) {
            Log.e("EjercicioViewModel", "Fallo al insertar ejercicio: ${e.message}", e)
            // Re-lanza la excepción para que la Activity pueda mostrar el Toast de error.
            throw e
        }
    }

    // ❌ ELIMINAMOS LA FUNCIÓN AGREGAR EJERCICIO:
    // Esta función local ya no es necesaria, el guardado en la BD es suficiente.
}