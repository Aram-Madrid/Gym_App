package com.example.ut2_app.fragments

import android.R
import android.content.Intent
import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.example.ut2_app.activities.ConfiguracionActivity
import com.example.ut2_app.databinding.FragmentHomeBinding
import com.example.ut2_app.viewmodels.HomeViewModel
import com.example.ut2_app.model.PuntosGrupoUI
import io.github.koalaplot.core.ChartLayout
import io.github.koalaplot.core.polar.*
import io.github.koalaplot.core.style.*
import io.github.koalaplot.core.util.ExperimentalKoalaPlotApi
import io.github.koalaplot.core.util.generateHueColorPalette

// -----------------------------------------------------------
// CLASE FRAGMENT
// -----------------------------------------------------------

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    // Inicializa el ViewModel
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
        binding.composeView.setContent {
            // Pasa el ViewModel al composable
            GraficoRadar(viewModel = viewModel)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

// -----------------------------------------------------------
// FUNCIÓN COMPOSABLE (Gráfico de Radar)
// -----------------------------------------------------------

@OptIn(ExperimentalKoalaPlotApi::class)
@Composable
fun GraficoRadar(viewModel: HomeViewModel) {

    val puntosData = viewModel.puntosRendimiento.collectAsState().value

    if (puntosData.isEmpty()) {
        Text(
            text = "Cargando datos de rendimiento...",
            fontSize = 18.sp,
            color = Color.Black
        )
        return
    }

    // 1. Mapear y convertir datos (usando Double)
    val categories = puntosData.map { it.grupo }
    val maxPoints = puntosData.maxOfOrNull { it.maximo } ?: 5.0
    val radialMax = maxPoints + 1.0

    val data: List<List<PolarPoint<Float, String>>> = listOf(
        puntosData.map { item ->
            DefaultPolarPoint(item.valor.toFloat(), item.grupo)
        }
    )

    val seriesColor = Color.Black // ⬅️ Usaremos Negro para el polígono de datos
    val radialRange = (0..radialMax.toInt()).map { it.toFloat() }
    val niveles = mapOf(
        0 to "F",
        (radialMax * 0.25).toInt() to "C",
        (radialMax * 0.50).toInt() to "B",
        (radialMax * 0.75).toInt() to "A",
        radialMax.toInt() to "S"
    )

    // 2. Renderizado del gráfico
    ChartLayout(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            // Fondo EXTERNO Transparente (pero el XML fuerza blanco debajo)
            .background(Color.Transparent)
    ) {
        PolarGraph(
            radialAxisModel = rememberFloatRadialAxisModel(radialRange),
            angularAxisModel = rememberCategoryAngularAxisModel(categories),

            // --- Etiquetas NEGRAS (Alto Contraste) ---
            radialAxisLabels = { valor ->
                Text(
                    text = niveles[valor.toInt()] ?: "",
                    color = Color.Black, // ⬅️ NEGRO PURO
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    fontStyle = FontStyle.Italic,
                    modifier = Modifier.padding(start = 11.dp)
                )
            },
            angularAxisLabels = { categoria ->
                Text(
                    text = categoria,
                    color = Color.Blue, // ⬅️ NEGRO PURO
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium
                )
            },

            polarGraphProperties = PolarGraphDefaults.PolarGraphPropertyDefaults().copy(
                radialGridType = RadialGridType.CIRCLES,

                // 🔑 SOLUCIÓN: Líneas GRISES (Radial Grid Lines - Circulares)
                radialAxisGridLineStyle = LineStyle(
                    SolidColor(Color.LightGray), // ⬅️ Forzamos el color Gris
                    strokeWidth = 1.dp,
                    alpha = 0.8f
                ),
                // Líneas GRISES (Angular Grid Lines - Radiales)
                angularAxisGridLineStyle = LineStyle(
                    SolidColor(Color.Black), // ⬅️ Forzamos el color Gris
                    strokeWidth = 1.dp,
                    alpha = 0.8f
                ),

                // Fondo INTERNO Transparente
                background = AreaStyle(SolidColor(Color.Transparent))
            )
        ) {
            // Dibuja el polígono de datos
            data.forEach { seriesData ->
                PolarPlotSeries(
                    seriesData,
                    // Línea del pentágono en negro
                    lineStyle = LineStyle(SolidColor(Color.Black), strokeWidth = 2.dp),
                    // Área totalmente transparente
                    areaStyle = AreaStyle(SolidColor(Color.Transparent), alpha = 0.0f)
                )
            }
        }
    }
}