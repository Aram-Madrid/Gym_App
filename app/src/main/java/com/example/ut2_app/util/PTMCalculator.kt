package com.example.ut2_app.util

import kotlin.math.pow

object PTMCalculator {

    fun calcularPTM(peso: Double, reps: Int, dificultad: Double = 1.0): Double {
        if (peso <= 0 || reps <= 0) return 0.0
        return (peso * reps * dificultad) / 10.0
    }

    fun calcularPTMConSeries(
        series: List<Pair<Double, Int>>,
        dificultad: Double = 1.0
    ): Double {
        if (series.isEmpty()) return 0.0
        return series.sumOf { (peso, reps) ->
            (peso * reps * dificultad) / 10.0
        }
    }

    /**
     * 🔑 FUNCIÓN RESTAURADA (COMPATIBILIDAD)
     * Se usa en EjercicioViewModel. Asume que no hay historial (ptmAnterior = 0.0).
     */
    fun calcularCambioELO(ptm: Double, eloActual: Int): Int {
        return calcularCambioEloProgresivo(ptm, 0.0, eloActual)
    }

    /**
     * Lógica Progresiva (Semanal)
     */
    fun calcularCambioEloProgresivo(
        ptmActual: Double,
        ptmAnterior: Double,
        eloActual: Int
    ): Int {
        if (ptmAnterior == 0.0) return 25 // Bonus bienvenida

        val diferencia = ptmActual - ptmAnterior
        val kFactor = when {
            eloActual < 1000 -> 30.0
            eloActual < 2000 -> 20.0
            else -> 10.0
        }

        return when {
            diferencia > 0 -> {
                val mejora = (diferencia / ptmAnterior)
                (kFactor * (1.0 + mejora)).toInt().coerceIn(5, 50)
            }
            diferencia == 0.0 -> if (eloActual < 1500) 0 else -2
            else -> {
                if (eloActual < 800) 0 else {
                    val perdida = (diferencia / ptmAnterior)
                    (kFactor * 2.0 * perdida).toInt().coerceIn(-40, -1)
                }
            }
        }
    }

    fun aplicarCambioELO(eloActual: Int, cambio: Int): Int {
        return (eloActual + cambio).coerceIn(0, 10000)
    }

    fun obtenerRango(elo: Int): String {
        return when {
            elo < 500 -> "Cobre"
            elo < 1000 -> "Bronce"
            elo < 1500 -> "Plata"
            elo < 2000 -> "Oro"
            elo < 2500 -> "Esmeralda"
            elo < 3000 -> "Diamante"
            else -> "Campeón"
        }
    }

    fun obtenerColorRango(elo: Int): String {
        return when {
            elo < 500 -> "#CD7F32"
            elo < 1000 -> "#CD853F"
            elo < 1500 -> "#C0C0C0"
            elo < 2000 -> "#FFD700"
            elo < 2500 -> "#50C878"
            elo < 3000 -> "#B9F2FF"
            else -> "#FF6B35"
        }
    }

    fun calcular1RM(peso: Double, reps: Int): Double {
        if (reps <= 0 || peso <= 0) return 0.0
        if (reps == 1) return peso
        return peso * (1 + reps / 30.0)
    }
}