package com.example.ut2_app

import android.content.Intent
import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
import com.example.ut2_app.activities.ConfiguracionActivity
import com.example.ut2_app.databinding.FragmentHomeBinding
import io.github.koalaplot.core.ChartLayout
import io.github.koalaplot.core.polar.*
import io.github.koalaplot.core.style.*
import io.github.koalaplot.core.util.ExperimentalKoalaPlotApi
import io.github.koalaplot.core.util.generateHueColorPalette
import kotlin.random.Random

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

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
            GraficoRadar()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

@OptIn(ExperimentalKoalaPlotApi::class)
@Composable
fun GraficoRadar() {
    val contextFondo = LocalContext.current
    val typedValue = TypedValue()
    contextFondo.theme.resolveAttribute(android.R.attr.colorAccent, typedValue, true)
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

            // --- Etiquetas radiales (letras S, A, B, C, F) ---
            radialAxisLabels = { valor ->
                Text(
                    text = niveles[valor.toInt()] ?: "",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    fontStyle = FontStyle.Italic,
                    modifier = Modifier.padding(start = 11.dp)
                )
            },

            // --- Etiquetas angulares (categorías) ---
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
