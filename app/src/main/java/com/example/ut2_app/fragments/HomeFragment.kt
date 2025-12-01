package com.example.ut2_app.fragments

import android.R
import android.content.Intent
import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast // Importación necesaria para mostrar errores
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import coil.load
import com.example.ut2_app.activities.ConfiguracionActivity
import com.example.ut2_app.databinding.FragmentHomeBinding
import com.example.ut2_app.util.SupabaseClientProvider
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import io.github.koalaplot.core.ChartLayout
import io.github.koalaplot.core.polar.*
import io.github.koalaplot.core.style.*
import io.github.koalaplot.core.util.ExperimentalKoalaPlotApi
import io.github.koalaplot.core.util.generateHueColorPalette
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlin.random.Random

// DATA CLASS ACTUALIZADA: Se añade el campo 'nombre'
@Serializable
data class PerfilUsuario(
    val id: String,
    val nombre: String, // <<--- CAMBIO CLAVE: Incluir el nombre
    val fotoperfilurl: String? = null,
    val elo: Int
)

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private val supabase = SupabaseClientProvider.supabase

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)

        binding.botonConfiguracion.setOnClickListener {
            val intent = Intent(requireContext(), ConfiguracionActivity::class.java)
            startActivity(intent)
        }

        // Llamar a la función que ahora carga todos los datos
        cargarDatosUsuario()

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.composeView.setContent {
            GraficoRadar()
        }
    }

    private fun cargarDatosUsuario() {
        val userId = supabase.auth.currentUserOrNull()?.id ?: return

        lifecycleScope.launch {
            try {
                // PASO 1: Obtener TODOS los usuarios para calcular el ranking global
                val allUsersResponse = supabase.postgrest["usuarios"]
                    .select()
                    .decodeList<PerfilUsuario>()

                // PASO 2: Ordenar la lista por ELO de forma descendente (Clasificación)
                val sortedUsers = allUsersResponse.sortedByDescending { it.elo }

                // PASO 3: Encontrar la posición del usuario actual en la lista ordenada
                // El ranking es el índice + 1
                val userPosition = sortedUsers.indexOfFirst { it.id == userId } + 1

                // PASO 4: Obtener los datos específicos del usuario actual
                val userData = allUsersResponse.firstOrNull { it.id == userId }

                if (userData != null) {
                    // Asignar Nombre
                    binding.nombreUsuario.text = userData.nombre

                    // Asignar Ranking (NUEVA LÓGICA)
                    binding.rankingUsuario.text = "Ranking: nº $userPosition"

                    // Cargar Foto (Lógica existente)
                    val url = userData.fotoperfilurl
                    if (!url.isNullOrEmpty()) {
                        // Cache busting para forzar recarga (buena práctica)
                        val cacheBustingUrl = "$url?v=${System.currentTimeMillis()}"

                        binding.emblemaRanking.load(cacheBustingUrl) {
                            crossfade(true)
                            error(com.example.ut2_app.R.drawable.place_holder)
                            placeholder(com.example.ut2_app.R.drawable.place_holder)
                        }
                    } else {
                        binding.emblemaRanking.setImageResource(com.example.ut2_app.R.drawable.place_holder)
                    }
                } else {
                    binding.nombreUsuario.text = "Usuario no encontrado"
                    binding.rankingUsuario.text = "Ranking: N/A"
                }

            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Error al cargar datos o ranking: ${e.message}", Toast.LENGTH_LONG).show()
                binding.nombreUsuario.text = "Error de carga"
                binding.rankingUsuario.text = "Ranking: N/A"
                binding.emblemaRanking.setImageResource(com.example.ut2_app.R.drawable.place_holder)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

// El código del @Composable GraficoRadar() no necesita cambios y se mantiene igual.
@OptIn(ExperimentalKoalaPlotApi::class)
@Composable
fun GraficoRadar() {
    val contextFondo = LocalContext.current
    val typedValue = TypedValue()
    contextFondo.theme.resolveAttribute(R.attr.colorAccent, typedValue, true)
    val accentColor = Color(typedValue.data)

    val categories = listOf("Pecho", "Brazos", "Core", "Espalda", "Piernas")
    val seriesNames = listOf("")

    val data: List<List<PolarPoint<Float, String>>> = buildList {
        seriesNames.forEach { _ ->
            add(categories.map { category ->
                DefaultPolarPoint(Random.nextDouble(1.0, 4.0).toFloat(), category)
            })
        }
    }

    val niveles = mapOf(
        0 to "F",
        1 to "C",
        2 to "B",
        3 to "A",
        4 to "S"
    )

    val palette = generateHueColorPalette(seriesNames.size)

    ChartLayout(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        PolarGraph(
            radialAxisModel = rememberFloatRadialAxisModel((0..4).map { it.toFloat() }),
            angularAxisModel = rememberCategoryAngularAxisModel(categories),

            radialAxisLabels = { valor ->
                Text(
                    text = niveles[valor.toInt()] ?: "",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    fontStyle = FontStyle.Italic,
                    modifier = Modifier.padding(start = 11.dp)
                )
            },

            angularAxisLabels = { categoria ->
                Text(
                    text = categoria,
                    color = Color(0xFF0099CC),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium
                )
            },

            polarGraphProperties = PolarGraphDefaults.PolarGraphPropertyDefaults().copy(
                radialGridType = RadialGridType.CIRCLES,
                angularAxisGridLineStyle = LineStyle(SolidColor(Color.Black), 4.dp, alpha = 0.3f),
                radialAxisGridLineStyle = LineStyle(SolidColor(Color.Black), 4.dp, alpha = 0.3f),
                background = AreaStyle(SolidColor(Color.Unspecified))
            )
        ) {
            data.forEachIndexed { index, seriesData ->
                PolarPlotSeries(
                    seriesData,
                    lineStyle = LineStyle(SolidColor(palette[index]), strokeWidth = 2.dp),
                    areaStyle = AreaStyle(SolidColor(palette[index]), alpha = 0.3f)
                )
            }
        }
    }
}