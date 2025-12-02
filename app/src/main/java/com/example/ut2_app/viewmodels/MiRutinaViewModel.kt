package com.example.ut2_app.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ut2_app.model.Rutina
import com.example.ut2_app.model.RutinaDia
import com.example.ut2_app.model.DiaSemanaUI
import com.example.ut2_app.model.RutinaDisplayItem
import com.example.ut2_app.util.AuthManager
import com.example.ut2_app.util.SupabaseClientProvider
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns.Companion.list
import kotlinx.coroutines.launch
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import android.util.Log
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID
import java.util.concurrent.TimeUnit

private val DIAS_SEMANA_FIJOS = listOf("Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado", "Domingo")

class MiRutinaViewModel : ViewModel() {

    private val _itemsRutina = MutableLiveData<List<RutinaDisplayItem>>(emptyList())
    val itemsRutina: LiveData<List<RutinaDisplayItem>> = _itemsRutina

    private val _isLoading = MutableLiveData(true)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>(null)
    val error: LiveData<String?> = _error

    private var idRutinaActual: String? = null

    init {
        cargarRutinas()
    }

    private suspend fun getActiveDaysFromSupabase(): List<RutinaDia> {
        val postgrestClient = SupabaseClientProvider.supabase.postgrest
        val currentUserId = AuthManager.getCurrentUserId() ?: return emptyList()

        val joinColumns = "id_rutina, id_usuario, nombre_rutina, rutina_dia(id_rutina, id_dia, dia_nombre, created_at)"

        val resultadoCompleto = postgrestClient["rutina"]
            .select(list(joinColumns)) {
                filter { eq("id_usuario", currentUserId) }
            }
            .decodeList<Rutina>()

        if (resultadoCompleto.isNotEmpty()) {
            idRutinaActual = resultadoCompleto.first().idRutina
        }

        // 🔑 ORDEN ASCENDENTE: De lo más antiguo (Semana 1) a lo más nuevo
        return resultadoCompleto.flatMap { it.rutinaDias }
            .sortedBy { it.fecha }
    }

    fun cargarRutinas() {
        _isLoading.value = true
        _error.value = null

        viewModelScope.launch {
            try {
                val diasHistorial = getActiveDaysFromSupabase()
                val listaVisual = mutableListOf<RutinaDisplayItem>()

                val calendar = Calendar.getInstance()
                calendar.firstDayOfWeek = Calendar.MONDAY
                // Formato ISO para las fechas objetivo
                val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
                val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())

                // 1. FECHA DE INICIO (Día 0 absoluto)
                val primerDiaDeTodos = diasHistorial.minByOrNull { it.fecha ?: "" }
                val fechaInicio = try {
                    if (primerDiaDeTodos?.fecha != null) dateFormat.parse(primerDiaDeTodos.fecha) else Date()
                } catch (e: Exception) { Date() }

                // Calendario apuntando al Lunes de la Semana 1
                val calInicio = Calendar.getInstance().apply {
                    time = fechaInicio ?: Date()
                    set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
                    set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
                }

                // 2. AGRUPAR HISTORIAL (Semana a Semana)
                val diasAgrupados = diasHistorial.groupBy { dia ->
                    val fecha = try {
                        if (dia.fecha != null) dateFormat.parse(dia.fecha) else Date()
                    } catch (e: Exception) { Date() }

                    calendar.time = fecha ?: Date()
                    val year = calendar.get(Calendar.YEAR)
                    val week = calendar.get(Calendar.WEEK_OF_YEAR)
                    "$year-$week"
                }

                var ultimaSemanaNumero = 0
                var ultimaFechaSemana: Date = calInicio.time

                // A. PROCESAR HISTORIAL
                if (diasAgrupados.isNotEmpty()) {
                    diasAgrupados.forEach { (_, diasDeEsaSemana) ->
                        val diaReferencia = diasDeEsaSemana.first()
                        val fechaRef = try { dateFormat.parse(diaReferencia.fecha ?: "") } catch(e:Exception){ Date() }

                        val calActual = Calendar.getInstance().apply {
                            time = fechaRef ?: Date()
                            set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
                            set(Calendar.HOUR_OF_DAY, 12)
                        }
                        ultimaFechaSemana = calActual.time // Guardamos referencia para la "Siguiente Semana"

                        // Calcular Semana Relativa (1, 2, 3...)
                        val diffMillis = calActual.timeInMillis - calInicio.timeInMillis
                        val numeroSemana = (TimeUnit.MILLISECONDS.toDays(diffMillis) / 7 + 1).toInt()
                        ultimaSemanaNumero = numeroSemana

                        // Header
                        val sdfDisplay = SimpleDateFormat("dd MMM", Locale.getDefault())
                        val finSemana = Calendar.getInstance().apply { time = calActual.time; add(Calendar.DAY_OF_YEAR, 6) }
                        listaVisual.add(RutinaDisplayItem.CabeceraSemana("Semana $numeroSemana", "${sdfDisplay.format(calActual.time)} - ${sdfDisplay.format(finSemana.time)}"))

                        // Items
                        val diasMap = diasDeEsaSemana.associateBy { it.diaNombre }
                        DIAS_SEMANA_FIJOS.forEachIndexed { index, nombreFijo ->
                            val diaReal = diasMap[nombreFijo]

                            // Calcular fecha objetivo para rellenar huecos en esta semana pasada
                            val calDia = Calendar.getInstance().apply { time = calActual.time; add(Calendar.DAY_OF_YEAR, index) }

                            listaVisual.add(RutinaDisplayItem.ItemDia(DiaSemanaUI(
                                nombreDia = nombreFijo,
                                idDiaRutina = diaReal?.idDia,
                                isActive = diaReal != null,
                                fechaObjetivo = isoFormat.format(calDia.time)
                            )))
                        }
                    }
                } else {
                    // Si está vacío, la "última semana" es la actual (Semana 1) pero sin pintar
                    // Para que el bloque "Futuro" pinte la Semana 1 directamente
                    ultimaSemanaNumero = 0
                    // Ajustamos fecha para que al sumar 1 semana abajo, quede en la actual
                    val calTemp = Calendar.getInstance()
                    calTemp.time = calInicio.time
                    calTemp.add(Calendar.WEEK_OF_YEAR, -1)
                    ultimaFechaSemana = calTemp.time
                }

                // B. AÑADIR SIEMPRE LA "SIGUIENTE SEMANA" (Futuro inmediato)
                val calSiguiente = Calendar.getInstance().apply {
                    time = ultimaFechaSemana
                    add(Calendar.WEEK_OF_YEAR, 1) // Sumamos 1 semana a la última conocida
                    set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
                }

                val siguienteNumero = ultimaSemanaNumero + 1
                val sdfDisplay = SimpleDateFormat("dd MMM", Locale.getDefault())
                val finSiguiente = Calendar.getInstance().apply { time = calSiguiente.time; add(Calendar.DAY_OF_YEAR, 6) }

                listaVisual.add(RutinaDisplayItem.CabeceraSemana("Semana $siguienteNumero", "${sdfDisplay.format(calSiguiente.time)} - ${sdfDisplay.format(finSiguiente.time)}"))

                // Generar los 7 días vacíos de la siguiente semana
                DIAS_SEMANA_FIJOS.forEachIndexed { index, nombreFijo ->
                    val calDia = Calendar.getInstance().apply {
                        time = calSiguiente.time
                        add(Calendar.DAY_OF_YEAR, index)
                    }

                    listaVisual.add(RutinaDisplayItem.ItemDia(DiaSemanaUI(
                        nombreDia = nombreFijo,
                        idDiaRutina = null, // Vacío
                        isActive = false,
                        fechaObjetivo = isoFormat.format(calDia.time) // 🔑 Fecha futura calculada
                    )))
                }

                _itemsRutina.postValue(listaVisual)

            } catch (e: Exception) {
                Log.e("MiRutinaViewModel", "Error: ${e.message}", e)
                _error.postValue("Error cargando rutina")
            } finally {
                _isLoading.postValue(false)
            }
        }
    }

    /**
     * Crea un día con la fecha exacta que seleccionó el usuario.
     */
    suspend fun crearRutinaDia(nombreDia: String, fechaObjetivoIso: String): String? {
        return try {
            val postgrestClient = SupabaseClientProvider.supabase.postgrest
            val currentUserId = AuthManager.getCurrentUserId() ?: return null

            var rutinaId = idRutinaActual
            if (rutinaId == null) {
                rutinaId = crearRutinaPrincipal(currentUserId) ?: return null
                idRutinaActual = rutinaId
            }

            val nuevoIdDia = UUID.randomUUID().toString()

            val rutinaDiaInsert = RutinaDiaInsert(
                id_dia = nuevoIdDia,
                id_rutina = rutinaId,
                dia_nombre = nombreDia,
                puntos_total = 0.0,
                created_at = fechaObjetivoIso // 🔑 Usamos la fecha que calculó el ViewModel
            )

            postgrestClient["rutina_dia"].insert(rutinaDiaInsert)
            cargarRutinas()
            nuevoIdDia
        } catch (e: Exception) {
            Log.e("MiRutinaViewModel", "Error crear dia: ${e.message}")
            null
        }
    }

    private suspend fun crearRutinaPrincipal(userId: String): String? {
        return try {
            val postgrestClient = SupabaseClientProvider.supabase.postgrest
            val nuevoIdRutina = UUID.randomUUID().toString()
            val rutinaInsert = RutinaInsert(nuevoIdRutina, userId, "Mi Rutina")
            postgrestClient["rutina"].insert(rutinaInsert)
            nuevoIdRutina
        } catch (e: Exception) { null }
    }
}

@Serializable
data class RutinaInsert(
    @SerialName("id_rutina") val id_rutina: String,
    @SerialName("id_usuario") val id_usuario: String,
    @SerialName("nombre_rutina") val nombre_rutina: String
)

@Serializable
data class RutinaDiaInsert(
    @SerialName("id_dia") val id_dia: String,
    @SerialName("id_rutina") val id_rutina: String,
    @SerialName("dia_nombre") val dia_nombre: String,
    @SerialName("puntos_total") val puntos_total: Double,
    @SerialName("created_at") val created_at: String? = null
)