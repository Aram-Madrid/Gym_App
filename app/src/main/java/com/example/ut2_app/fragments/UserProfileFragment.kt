package com.example.ut2_app.fragments

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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import coil.load
import com.example.ut2_app.R
import com.example.ut2_app.databinding.FragmentUserProfileBinding
import com.example.ut2_app.model.PuntosGrupoUI
import com.example.ut2_app.viewmodels.UserProfileViewModel
import io.github.koalaplot.core.ChartLayout
import io.github.koalaplot.core.polar.*
import io.github.koalaplot.core.style.*
import io.github.koalaplot.core.util.ExperimentalKoalaPlotApi
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class UserProfileFragment : Fragment() {

    private var _binding: FragmentUserProfileBinding? = null
    private val binding get() = _binding!!

    private val viewModel: UserProfileViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentUserProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnVolver.setOnClickListener {
            findNavController().popBackStack()
        }

        val userId = arguments?.getString("USER_ID")

        if (userId != null) {
            viewModel.cargarPerfil(userId)
        }

        lifecycleScope.launch {
            viewModel.usuarioSeleccionado.collectLatest { usuario ->
                if (usuario != null) {
                    binding.nombreUsuario.text = usuario.nombre

                    val rangoTexto = usuario.rango ?: "Cobre"
                    binding.rankingUsuario.text = "${usuario.elo ?: 0} ELO - $rangoTexto"

                    if (!usuario.fotoPerfilUrl.isNullOrEmpty()) {
                        binding.emblemaRanking.load(usuario.fotoPerfilUrl) {
                            crossfade(true)
                            placeholder(R.drawable.place_holder)
                            error(R.drawable.place_holder)
                        }
                    } else {
                        binding.emblemaRanking.setImageResource(R.drawable.place_holder)
                    }
                }
            }
        }

        binding.composeView.setContent {
            val puntosData = viewModel.puntosRendimiento.collectAsState().value
            val isLoading = viewModel.isLoading.collectAsState().value

            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Color(0xFFffa31a)) // Tu color primario
                }
            } else {
                MostrarGraficoRadarPerfil(puntosData)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}


@OptIn(ExperimentalKoalaPlotApi::class)
@Composable
fun MostrarGraficoRadarPerfil(puntosData: List<PuntosGrupoUI>) {

    if (puntosData.isEmpty()) return

    val categories = puntosData.map { it.grupo }
    val maxRadial = 5f

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

    val radialRange = listOf(0f, 1f, 2f, 3f, 4f, 5f)
    val niveles = mapOf(0 to "F", 1 to "D", 2 to "C", 3 to "B", 4 to "A", 5 to "S")

    ChartLayout(
        modifier = Modifier
            .fillMaxSize()
            .padding(10.dp)
            .background(Color.Transparent)
    ) {
        PolarGraph(
            radialAxisModel = rememberFloatRadialAxisModel(radialRange),
            angularAxisModel = rememberCategoryAngularAxisModel(categories),
            radialAxisLabels = { valor ->
                val nivel = niveles[valor.toInt()] ?: ""
                Text(
                    text = nivel,
                    color = Color.White, // Blanco para modo oscuro
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 2.dp)
                )
            },
            angularAxisLabels = { categoria ->
                Text(
                    text = categoria,
                    color = Color(0xFFffa31a), // Tu Naranja
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
            },
            polarGraphProperties = PolarGraphDefaults.PolarGraphPropertyDefaults().copy(
                radialGridType = RadialGridType.CIRCLES,
                radialAxisGridLineStyle = LineStyle(
                    SolidColor(Color.Gray),
                    strokeWidth = 1.dp,
                    alpha = 0.5f
                ),
                angularAxisGridLineStyle = LineStyle(
                    SolidColor(Color.Gray),
                    strokeWidth = 1.dp,
                    alpha = 0.3f
                ),
                background = AreaStyle(SolidColor(Color.Transparent))
            )
        ) {
            datosNormalizados.forEach { seriesData ->
                PolarPlotSeries(
                    seriesData,
                    lineStyle = LineStyle(
                        SolidColor(Color(0xFFffa31a)), // Tu Naranja
                        strokeWidth = 2.dp
                    ),
                    areaStyle = AreaStyle(
                        SolidColor(Color(0xFFffa31a)),
                        alpha = 0.3f
                    )
                )
            }
        }
    }
}