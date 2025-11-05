package com.example.ut2_app

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import com.example.ut2_app.databinding.FragmentHomeBinding
import io.github.koalaplot.core.ChartLayout

import io.github.koalaplot.core.polar.*
import io.github.koalaplot.core.style.*
import io.github.koalaplot.core.util.ExperimentalKoalaPlotApi
import io.github.koalaplot.core.util.generateHueColorPalette
import kotlin.random.Random

class HomeFragment : Fragment() {

    // con ViewBinding hago referencia al archivo XML fragment_home.xml
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Enlaza el layout fragment_home.xml con el binding
        _binding = FragmentHomeBinding.inflate(inflater, container, false)

        // Configuro el botón para abrir ConfiguracionActivity
        binding.botonConfiguracion.setOnClickListener {
            val intent = Intent(requireContext(), ConfiguracionActivity::class.java)
            startActivity(intent)
        }

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Mediante el binding.composeView.setContent renderizo el contenido del ComposeView
        binding.composeView.setContent {
            // Creo el método GraficoRadar() que contiene toda la lógica del gráfico
            GraficoRadar()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

// ----------------------------------------------------
//                  Generación del gráfico
// -----------------------------------------------------
@OptIn(ExperimentalKoalaPlotApi::class)
@Composable
fun GraficoRadar() {

    // Listo las categorias del grafico
    val categories = listOf("Pecho", "Brazos", "Core", "Espalda", "Piernas")

    // Sirve para listar la leyenda del grafico, como no quiero que tenga leyenda lo dejo vacío
    val seriesNames = listOf("")

    // Genero los datos aleatorios para cada categoría (1.0 a 5.0)
    val data: List<List<PolarPoint<Float, String>>> = buildList {
        seriesNames.forEach {
            add(
                categories.map { category ->
                    DefaultPolarPoint(Random.nextDouble(1.0, 4.0).toFloat(), category)
                }
            )
        }
    }

    // Este mapa sirve para convertir los valores numéricos del eje radial en letras
    val niveles = mapOf(
        0 to "F",
        1 to "C",
        2 to "B",
        3 to "A",
        4 to "S"
    )


    val palette = generateHueColorPalette(seriesNames.size)

    // Margen interno para los elementos
    val padding = 8.dp


    // ----------------------------------------------------
    //      Estructura del gráfico mediante ChartLayout
    // ----------------------------------------------------
    ChartLayout(
        modifier = Modifier
            //Sirve para que el gráfico ocupe todo el espacio posible
            .fillMaxSize()
            // Deja un margen alrededor
            .padding(16.dp)
    ) {


        // ----------------------------------------------------
        //   Contenido del propio gráfico mediante PolarGraph
        // ----------------------------------------------------
        PolarGraph(
            // Eje radial: valores de 0 a 5
            rememberFloatRadialAxisModel((0..4).map { it.toFloat() }),

            // Eje angular: las categorías que he definido antes
            rememberCategoryAngularAxisModel(categories),

            // Muestra las letras del mapa "niveles"
            radialAxisLabelText = { valor -> niveles[valor.toInt()] ?: "" },

            // Muestra los nombres de las categorías en el eje angular
            angularAxisLabelText = { it },

            // Propiedades visuales del gráfico
            polarGraphProperties = PolarGraphDefaults.PolarGraphPropertyDefaults().copy(
                // Hace que la rejilla sea circular (en vez de líneas rectas)
                radialGridType = RadialGridType.CIRCLES,

                // Estilo de las líneas del eje angular
                angularAxisGridLineStyle = LineStyle(SolidColor(Color.Black), 2.dp, alpha = 0.4f),

                // Estilo de las líneas del eje radial
                radialAxisGridLineStyle = LineStyle(SolidColor(Color.Black), 2.dp, alpha = 0.3f)
            )
        ) {


            // -------------------------------------------------------------
            // Dibuja los valores de cada categoría mediante PolarPlotSeries
            // -------------------------------------------------------------
            data.forEachIndexed { index, seriesData ->
                PolarPlotSeries(
                    // Lista de puntos (uno por categoría)
                    seriesData,

                    // Estilo de la línea que une los puntos
                    lineStyle = LineStyle(SolidColor(palette[index]), strokeWidth = 2.dp),

                    // Relleno del área bajo la línea (transparente)
                    areaStyle = AreaStyle(SolidColor(palette[index]), alpha = 0.3f)
                )
            }
        }
    }
}
