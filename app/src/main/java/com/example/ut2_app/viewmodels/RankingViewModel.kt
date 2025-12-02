package com.example.ut2_app.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ut2_app.model.UsuarioRankingDB
import com.example.ut2_app.util.AuthManager
import com.example.ut2_app.util.SupabaseClientProvider
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.launch
import android.util.Log
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

class RankingViewModel : ViewModel() {

    private val _usuariosRanking = MutableLiveData<List<UsuarioRankingDB>>(emptyList())
    val usuariosRanking: LiveData<List<UsuarioRankingDB>> = _usuariosRanking

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private var currentUserId: String? = null

    init {
        // Esperamos a que el fragmento lo pida
    }

    fun cargarRanking() {
        viewModelScope.launch {
            _isLoading.postValue(true)
            try {
                currentUserId = AuthManager.getCurrentUserId()

                if (currentUserId == null) {
                    _usuariosRanking.postValue(emptyList())
                    _isLoading.postValue(false)
                    return@launch
                }

                val postgrestClient = SupabaseClientProvider.supabase.postgrest

                // 1. OBTENER LISTA DE AMIGOS (IDs)
                // Consultamos la tabla 'amigos' para ver a quién sigue el usuario
                val listaAmigos = try {
                    postgrestClient["amigos"]
                        .select(Columns.list("id_amigo")) {
                            filter { eq("id", currentUserId!!) }
                        }
                        .decodeList<AmigoDTO>()
                        .map { it.idAmigo }
                } catch (e: Exception) {
                    Log.e("RankingViewModel", "Error cargando amigos: ${e.message}")
                    emptyList()
                }

                // 2. CREAR LISTA FILTRADA (Yo + Mis Amigos)
                val idsAVisualizar = listaAmigos + currentUserId!!

                // 3. CONSULTAR SOLO ESOS USUARIOS
                val resultados = postgrestClient["usuarios"]
                    .select {
                        // Filtro CLAVE: Solo traer usuarios cuyo ID esté en mi lista
                        filter { isIn("id", idsAVisualizar) }
                        order("elo", Order.DESCENDING)
                    }
                    .decodeList<UsuarioRankingDB>()

                // 4. PROCESAR (Asignar posición 1, 2, 3... en este ranking privado)
                val listaProcesada = resultados.mapIndexed { index, usuario ->
                    usuario.copy(
                        posicion = index + 1,
                        esActual = usuario.id == currentUserId
                    )
                }

                _usuariosRanking.postValue(listaProcesada)
                Log.d("RankingViewModel", "Ranking cargado: ${listaProcesada.size} usuarios (Tú + ${listaAmigos.size} amigos)")

            } catch (e: Exception) {
                Log.e("RankingViewModel", "Error al cargar el ranking: ${e.message}", e)
                _usuariosRanking.postValue(emptyList())
            } finally {
                _isLoading.postValue(false)
            }
        }
    }
}

// DTO interno para leer la tabla 'amigos'
@Serializable
data class AmigoDTO(
    @SerialName("id_amigo") val idAmigo: String
)