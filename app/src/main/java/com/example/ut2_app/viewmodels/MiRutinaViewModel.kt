package com.example.ut2_app.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ut2_app.model.Rutina
import com.example.ut2_app.model.RutinaDia
import com.example.ut2_app.model.DiaSemanaUI
import com.example.ut2_app.util.AuthManager
import com.example.ut2_app.util.SupabaseClientProvider
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.launch
import android.util.Log
import io.github.jan.supabase.postgrest.query.Columns.Companion.list
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName
import java.util.UUID

private val DIAS_SEMANA_FIJOS = listOf("Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado", "Domingo")

class MiRutinaViewModel : ViewModel() {

    private val _diasSemana = MutableLiveData<List<DiaSemanaUI>>(emptyList())
    val diasSemana: LiveData<List<DiaSemanaUI>> = _diasSemana

    private val _isLoading = MutableLiveData(true)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>(null)
    val error: LiveData<String?> = _error

    private var idRutinaActual: String? = null // Para reutilizar la rutina principal

    init {
        cargarRutinas()
    }

    private suspend fun getActiveDaysFromSupabase(): List<RutinaDia> {
        val postgrestClient = SupabaseClientProvider.supabase.postgrest

        // 🔑 Obtener ID del usuario actual
        val currentUserId = AuthManager.getCurrentUserId()
        if (currentUserId == null) {
            Log.e("MiRutinaViewModel", "No hay usuario autenticado")
            return emptyList()
        }

        val joinColumns = "id_usuario,id_rutina, nombre_rutina, rutina_dia(id_rutina, id_dia, dia_nombre)"

        val resultadoCompleto = postgrestClient["rutina"]
            .select(list(joinColumns)) {
                filter { eq("id_usuario", currentUserId) }
            }
            .decodeList<Rutina>()

        // Guardar el ID de la rutina principal para crear días nuevos
        if (resultadoCompleto.isNotEmpty()) {
            idRutinaActual = resultadoCompleto.first().idRutina
        }

        return resultadoCompleto.flatMap { it.rutinaDias }
    }

    fun cargarRutinas() {
        _isLoading.value = true
        _error.value = null

        viewModelScope.launch {
            try {
                // 1. Obtener los días activos de la BD
                val listaDiasAplanada = getActiveDaysFromSupabase()

                // 2. Crear mapa: nombreDia → RutinaDia
                val diasActivosMap = listaDiasAplanada.associateBy {
                    it.diaNombre.split(" ").firstOrNull() ?: it.diaNombre
                }

                // 3. Combinar con los 7 días fijos
                val listaDiasCombinada = DIAS_SEMANA_FIJOS.map { nombreFijo ->
                    val diaActivo = diasActivosMap[nombreFijo]

                    DiaSemanaUI(
                        nombreDia = nombreFijo,
                        idDiaRutina = diaActivo?.idDia,
                        isActive = diaActivo != null
                    )
                }

                _diasSemana.postValue(listaDiasCombinada)
                Log.d("MiRutinaViewModel", "Cargados ${listaDiasCombinada.count { it.isActive }} días activos")

            } catch (e: Exception) {
                Log.e("MiRutinaViewModel", "Error al cargar días: ${e.message}", e)
                _error.postValue("Error al cargar rutinas: ${e.localizedMessage}")
            } finally {
                _isLoading.postValue(false)
            }
        }
    }

    /**
     * 🔑 FUNCIÓN NUEVA: Crear un nuevo día de rutina.
     *
     * @param nombreDia Nombre del día (ej: "Lunes")
     * @return ID del día creado o null si hubo error
     */
    suspend fun crearRutinaDia(nombreDia: String): String? {
        return try {
            val postgrestClient = SupabaseClientProvider.supabase.postgrest
            val currentUserId = AuthManager.getCurrentUserId()

            if (currentUserId == null) {
                Log.e("MiRutinaViewModel", "No hay usuario autenticado")
                return null
            }

            // 🔑 PASO 1: Verificar si existe rutina principal, si no, crearla
            var rutinaId = idRutinaActual

            if (rutinaId == null) {
                rutinaId = crearRutinaPrincipal(currentUserId)
                if (rutinaId == null) {
                    Log.e("MiRutinaViewModel", "No se pudo crear la rutina principal")
                    return null
                }
                idRutinaActual = rutinaId
            }

            // 🔑 PASO 2: Crear el día de rutina
            val nuevoIdDia = UUID.randomUUID().toString()

            val rutinaDiaInsert = RutinaDiaInsert(
                id_dia = nuevoIdDia,
                id_rutina = rutinaId,
                dia_nombre = nombreDia,
                puntos_total = 0.0
            )

            postgrestClient["rutina_dia"].insert(rutinaDiaInsert)

            Log.d("MiRutinaViewModel", "Día creado: $nombreDia con ID: $nuevoIdDia")

            // 🔑 PASO 3: Recargar la lista para actualizar la UI
            cargarRutinas()

            nuevoIdDia

        } catch (e: Exception) {
            Log.e("MiRutinaViewModel", "Error al crear día de rutina: ${e.message}", e)
            _error.postValue("Error al crear día: ${e.localizedMessage}")
            null
        }
    }

    /**
     * Crea la rutina principal si no existe.
     */
    private suspend fun crearRutinaPrincipal(userId: String): String? {
        return try {
            val postgrestClient = SupabaseClientProvider.supabase.postgrest
            val nuevoIdRutina = UUID.randomUUID().toString()

            val rutinaInsert = RutinaInsert(
                id_rutina = nuevoIdRutina,
                id_usuario = userId,
                nombre_rutina = "Mi Rutina"
            )

            postgrestClient["rutina"].insert(rutinaInsert)

            Log.d("MiRutinaViewModel", "Rutina principal creada con ID: $nuevoIdRutina")
            nuevoIdRutina

        } catch (e: Exception) {
            Log.e("MiRutinaViewModel", "Error al crear rutina principal: ${e.message}", e)
            null
        }
    }
}

// 🔑 DTOs para inserción en Supabase

@Serializable
data class RutinaInsert(
    @SerialName("id_rutina")
    val id_rutina: String,
    @SerialName("id_usuario")
    val id_usuario: String,
    @SerialName("nombre_rutina")
    val nombre_rutina: String
)

@Serializable
data class RutinaDiaInsert(
    @SerialName("id_dia")
    val id_dia: String,
    @SerialName("id_rutina")
    val id_rutina: String,
    @SerialName("dia_nombre")
    val dia_nombre: String,
    @SerialName("puntos_total")
    val puntos_total: Double
)