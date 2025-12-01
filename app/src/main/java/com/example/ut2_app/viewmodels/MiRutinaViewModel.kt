package com.example.ut2_app.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ut2_app.model.Rutina
import com.example.ut2_app.model.RutinaDia
import com.example.ut2_app.model.DiaSemanaUI
import com.example.ut2_app.util.SupabaseClientProvider
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.launch
import android.util.Log
import io.github.jan.supabase.postgrest.query.Columns.Companion.list

private val DIAS_SEMANA_FIJOS = listOf("Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado", "Domingo")

class MiRutinaViewModel : ViewModel() {

    private val currentUserId: String = "5e6ba799-6770-400d-b747-1989fe602ff2"

    private val _diasSemana = MutableLiveData<List<DiaSemanaUI>>(emptyList())
    val diasSemana: LiveData<List<DiaSemanaUI>> = _diasSemana

    private val _isLoading = MutableLiveData(true)
    val isLoading: LiveData<Boolean> = _isLoading

    init {
        cargarRutinas()
    }

    private suspend fun getActiveDaysFromSupabase(): List<RutinaDia> {
        val postgrestClient = SupabaseClientProvider.supabase.postgrest

        // 🔑 CORRECCIÓN CLAVE: Solicitamos 'id_rutina' dentro de la selección anidada de rutina_dia.
        val joinColumns =
            "id_usuario,id_rutina, nombre_rutina, rutina_dia(id_rutina, id_dia, dia_nombre)"

        val resultadoCompleto = postgrestClient["rutina"]
            .select(list(joinColumns)) {
                filter { eq("id_usuario", currentUserId) }
            }
            .decodeList<Rutina>()

        // Aplanamos y devolvemos solo los días activos (RutinaDia)
        return resultadoCompleto.flatMap { it.rutinaDias }
    }

    fun cargarRutinas() {
        _isLoading.value = true
        viewModelScope.launch {
            try {
                // 1. Obtener los días activos de la BD
                val listaDiasAplanada = getActiveDaysFromSupabase() // Lista de RutinaDia

                // 🔑 CORRECCIÓN CLAVE: Creamos un mapa extrayendo solo la primera palabra ("Lunes")
                val diasActivosMap = listaDiasAplanada.associateBy {
                    // Dividimos el string por espacios y tomamos el primer elemento (Ej: "Lunes - Pecho" -> "Lunes")
                    it.diaNombre.split(" ").firstOrNull() ?: it.diaNombre
                }

                // 2. FUSIÓN: Iterar sobre los 7 días fijos y añadir datos activos
                val listaDiasCombinada = DIAS_SEMANA_FIJOS.map { nombreFijo ->
                    val diaActivo = diasActivosMap[nombreFijo]

                    DiaSemanaUI(
                        nombreDia = nombreFijo,
                        idDiaRutina = diaActivo?.idDia,
                        isActive = diaActivo != null // Esto ahora será TRUE para Lunes, Miércoles, Viernes
                    )
                }

                _diasSemana.postValue(listaDiasCombinada)

            } catch (e: Exception) {
                Log.e("MiRutinaViewModel", "Error al cargar días combinados: ${e.message}")
            } finally {
                _isLoading.postValue(false)
            }
        }
    }
}