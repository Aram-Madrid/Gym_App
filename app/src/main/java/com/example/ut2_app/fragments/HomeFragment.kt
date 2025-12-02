package com.example.ut2_app.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import coil.load
import com.example.ut2_app.R
import com.example.ut2_app.activities.ConfiguracionActivity
import com.example.ut2_app.databinding.FragmentHomeBinding
import com.example.ut2_app.model.PuntosGrupoUI
import com.example.ut2_app.viewmodels.HomeViewModel
import com.example.ut2_app.viewmodels.UsuarioHome
import io.github.koalaplot.core.ChartLayout
import io.github.koalaplot.core.polar.*
import io.github.koalaplot.core.style.*
import io.github.koalaplot.core.util.ExperimentalKoalaPlotApi
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val viewModel: HomeViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)

        binding.botonConfiguracion.setOnClickListener {
            val intent = Intent(requireContext(), ConfiguracionActivity::class.java)
            startActivity(intent)
        }

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Observar datos del usuario
        observarDatosUsuario()

        // Configurar Compose para el gráfico
        binding.composeView.setContent {
            GraficoRadar(viewModel = viewModel)
        }
    }

    override fun onResume() {
        super.onResume()
        // Recargar datos cada vez que el fragment es visible
        viewModel.recargarPuntos()
    }

    private fun observarDatosUsuario() {
        // Observar usuario actual
        lifecycleScope.launch {
            viewModel.usuarioActual.collectLatest { usuario ->
                if (usuario != null) {
                    actualizarUIUsuario(usuario)
                }
            }
        }

        // Observar posición en ranking
        lifecycleScope.launch {
            viewModel.posicionRanking.collectLatest { posicion ->
                binding.rankingUsuario.text = if (posicion != null) {
                    "Ranking #$posicion"
                } else {
                    "Ranking --"
                }
            }
        }
    }

    private fun actualizarUIUsuario(usuario: UsuarioHome) {
        // Nombre
        binding.nombreUsuario.text = usuario.nombre

        // Foto de perfil con Coil
        if (!usuario.fotoPerfilUrl.isNullOrEmpty()) {
            // Cache busting para evitar imágenes antiguas cacheadas
            val urlConCacheBusting = "${usuario.fotoPerfilUrl}?v=${System.currentTimeMillis()}"
            binding.emblemaRanking.load(urlConCacheBusting) {
                crossfade(true)
                placeholder(R.drawable.place_holder)
                error(R.drawable.place_holder)
            }
        } else {
            binding.emblemaRanking.setImageResource(R.drawable.place_holder)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

// COMPOSABLES - Gráfico de Radar

@OptIn(ExperimentalKoalaPlotApi::class)
@Composable
fun GraficoRadar(viewModel: HomeViewModel) {

    val puntosData = viewModel.puntosRendimiento.collectAsState().value
    val isLoading = viewModel.isLoading.collectAsState().value

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Transparent),
        contentAlignment = Alignment.Center
    ) {
        when {
            isLoading -> {
                CircularProgressIndicator(
                    color = Color(0xFF4CAF50)
                )
            }
            puntosData.isEmpty() -> {
                Text(
                    text = "Aún no tienes datos de rendimiento.\n¡Empieza a entrenar!",
                    fontSize = 16.sp,
                    color = Color.Gray,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(16.dp)
                )
            }
            else -> {
                MostrarGraficoRadar(puntosData)
            }
        }
    }
}

@OptIn(ExperimentalKoalaPlotApi::class)
@Composable
fun MostrarGraficoRadar(puntosData: List<PuntosGrupoUI>) {

    // Nombres de los grupos musculares
    val categories = puntosData.map { it.grupo }

    // Escala fija de 0 a 5 para los rangos (F, D, C, B, A, S)
    val maxRadial = 5f

    // Normalizar los valores al rango 0-5 basándose en el máximo de cada grupo
    val datosNormalizados: List<List<PolarPoint<Float, String>>> = listOf(
        puntosData.map { item ->
            val valorNormalizado = if (item.maximo > 0) {
                ((item.valor / item.maximo) * maxRadial).toFloat().coerceIn(0f, maxRadial)
            } else {
                0f
            }
            DefaultPolarPoint(valorNormalizado, item.grupo)
        }
    )

    // Rango fijo de 0 a 5
    val radialRange = listOf(0f, 1f, 2f, 3f, 4f, 5f)

    // Mapeo de valores a letras de rango
    val niveles = mapOf(
        0 to "F",
        1 to "D",
        2 to "C",
        3 to "B",
        4 to "A",
        5 to "S"
    )

    ChartLayout(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
            .background(Color.Transparent)
    ) {
        PolarGraph(
            radialAxisModel = rememberFloatRadialAxisModel(radialRange),
            angularAxisModel = rememberCategoryAngularAxisModel(categories),

            // Etiquetas radiales (F, D, C, B, A, S)
            radialAxisLabels = { valor ->
                val nivel = niveles[valor.toInt()] ?: ""
                Text(
                    text = nivel,
                    color = Color.Black,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 2.dp)
                )
            },

            // Etiquetas angulares (Pecho, Espalda, etc.)
            angularAxisLabels = { categoria ->
                Text(
                    text = categoria,
                    color = Color(0xFF1565C0),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            },

            polarGraphProperties = PolarGraphDefaults.PolarGraphPropertyDefaults().copy(
                radialGridType = RadialGridType.CIRCLES,
                radialAxisGridLineStyle = LineStyle(
                    SolidColor(Color.LightGray),
                    strokeWidth = 1.dp,
                    alpha = 0.6f
                ),
                angularAxisGridLineStyle = LineStyle(
                    SolidColor(Color.Gray),
                    strokeWidth = 1.dp,
                    alpha = 0.4f
                ),
                background = AreaStyle(SolidColor(Color.Transparent))
            )
        ) {
            datosNormalizados.forEach { seriesData ->
                PolarPlotSeries(
                    seriesData,
                    lineStyle = LineStyle(
                        SolidColor(Color(0xFF4CAF50)), // Verde
                        strokeWidth = 2.5.dp
                    ),
                    areaStyle = AreaStyle(
                        SolidColor(Color(0xFF4CAF50)),
                        alpha = 0.25f
                    )
                )
            }
        }
    }
}