package com.example.ut2_app.util

import kotlin.math.pow

/**
 * Utilidad para calcular PTM (Puntos de Trabajo Muscular) y ELO.
 *
 * PTM es una métrica que combina peso, repeticiones y dificultad para
 * cuantificar el trabajo realizado en un ejercicio.
 */
object PTMCalculator {

    /**
     * Calcula el PTM para un ejercicio completo.
     *
     * Fórmula simplificada: PTM = (Peso × Reps × Dificultad) / Factor
     *
     * @param peso Peso total o máximo usado
     * @param reps Repeticiones totales
     * @param dificultad Factor de dificultad (1.0 = normal, >1 = más difícil)
     * @return PTM calculado
     */
    fun calcularPTM(peso: Double, reps: Int, dificultad: Double = 1.0): Double {
        if (peso <= 0 || reps <= 0) return 0.0

        // Fórmula básica: Peso × Reps × Dificultad / 10
        val ptm = (peso * reps * dificultad) / 10.0

        return ptm
    }

    /**
     * Calcula el PTM considerando todas las series individualmente.
     * Esta es una versión más precisa que suma el PTM de cada serie.
     *
     * @param series Lista de pares (peso, repeticiones) para cada serie
     * @param dificultad Factor de dificultad del ejercicio
     * @return PTM total
     */
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
     * Calcula el cambio de ELO para el usuario basado en el PTM logrado.
     *
     * Esta función determina cuánto debe aumentar o disminuir el ELO del usuario.
     * El cambio se basa en:
     * - PTM logrado vs PTM esperado
     * - Nivel actual del usuario (ELO actual)
     *
     * @param ptmLogrado PTM del ejercicio realizado
     * @param eloActualUsuario ELO actual del usuario
     * @param ptmEsperado PTM que se esperaba lograr (basado en historial)
     * @return Cambio en ELO (puede ser positivo o negativo)
     */
    fun calcularCambioELO(
        ptmLogrado: Double,
        eloActualUsuario: Int = 1000,
        ptmEsperado: Double? = null
    ): Int {
        // Factor K: determina qué tan sensible es el cambio
        // Más alto para principiantes, más bajo para avanzados
        val kFactor = when {
            eloActualUsuario < 1200 -> 40.0 // Principiantes
            eloActualUsuario < 1800 -> 32.0 // Intermedios
            else -> 24.0 // Avanzados
        }

        // Si no hay PTM esperado, usar uno basado en el ELO actual
        val ptmEsperadoFinal = ptmEsperado ?: (eloActualUsuario / 10.0)

        // Calcular rendimiento: 1.0 = como esperado, >1 = mejor, <1 = peor
        val rendimiento = if (ptmEsperadoFinal > 0) {
            (ptmLogrado / ptmEsperadoFinal).coerceIn(0.0, 2.0)
        } else {
            1.0
        }

        // Calcular cambio base
        val cambioBase = when {
            rendimiento >= 1.5 -> kFactor * 1.0  // Excelente rendimiento
            rendimiento >= 1.2 -> kFactor * 0.7  // Buen rendimiento
            rendimiento >= 1.0 -> kFactor * 0.5  // Rendimiento esperado
            rendimiento >= 0.8 -> kFactor * 0.2  // Rendimiento bajo
            rendimiento >= 0.6 -> 0.0             // Muy bajo (sin cambio)
            else -> -kFactor * 0.3               // Extremadamente bajo (penalización)
        }

        // Bonus por alto PTM absoluto
        val bonusPTM = when {
            ptmLogrado >= 300 -> 10
            ptmLogrado >= 200 -> 5
            ptmLogrado >= 100 -> 2
            else -> 0
        }

        val cambioTotal = (cambioBase + bonusPTM).toInt()

        return cambioTotal.coerceIn(-50, 50) // Limitar cambio máximo
    }

    /**
     * Calcula el nuevo ELO del usuario después de un ejercicio.
     *
     * @param eloActual ELO actual del usuario
     * @param cambio Cambio calculado por calcularCambioELO()
     * @return Nuevo ELO (limitado entre 0 y 3000)
     */
    fun aplicarCambioELO(eloActual: Int, cambio: Int): Int {
        val nuevoElo = eloActual + cambio
        return nuevoElo.coerceIn(0, 3000)
    }

    /**
     * Determina el rango según el ELO.
     *
     * @param elo Valor de ELO
     * @return Nombre del rango
     */
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

    /**
     * Obtiene el color asociado al rango (para UI).
     * Retorna un código de color en formato hexadecimal.
     */
    fun obtenerColorRango(elo: Int): String {
        return when {
            elo < 500 -> "#CD7F32"      // Cobre - Marrón cobrizo
            elo < 1000 -> "#CD853F"     // Bronce - Marrón bronce
            elo < 1500 -> "#C0C0C0"     // Plata - Plateado
            elo < 2000 -> "#FFD700"     // Oro - Dorado
            elo < 2500 -> "#50C878"     // Esmeralda - Verde esmeralda
            elo < 3000 -> "#B9F2FF"     // Diamante - Azul diamante
            else -> "#FF6B35"           // Campeón - Naranja brillante
        }
    }

    /**
     * Calcula el One-Rep Max (1RM) usando la fórmula de Epley.
     * Útil para estimar el peso máximo que podrías levantar en 1 repetición.
     *
     * @param peso Peso usado
     * @param reps Repeticiones realizadas
     * @return 1RM estimado
     */
    fun calcular1RM(peso: Double, reps: Int): Double {
        if (reps <= 0 || peso <= 0) return 0.0
        if (reps == 1) return peso

        // Fórmula de Epley: 1RM = peso × (1 + reps/30)
        return peso * (1 + reps / 30.0)
    }
}