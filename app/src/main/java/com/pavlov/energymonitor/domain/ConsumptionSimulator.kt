package com.pavlov.energymonitor.domain

import kotlin.random.Random

/**
 * Симулятор смарт-лічильника. Базовий добовий профіль + випадкові коливання.
 * [random] можна підмінити детермінованим генератором у тестах.
 */
class ConsumptionSimulator(
    private val random: Random = Random.Default,
    val limitW: Int = DEFAULT_LIMIT_W,
) {
    /** Типовий добовий профіль домогосподарства, Вт (індекс = година доби). */
    val hourlyProfileW: List<Int> = listOf(
        350, 300, 280, 280, 300, 450, 900, 1600, 1400, 900, 800, 850,
        1100, 1000, 900, 950, 1300, 2100, 2900, 3100, 2600, 1800, 900, 500,
    )

    /** Нове вимірювання для вказаної години: базове значення ±15 %, напруга 230 В ±6 В. */
    fun nextReading(hour: Int): PowerReading {
        val base = hourlyProfileW[hour.coerceIn(0, 23)]
        val jitter = 0.85 + random.nextDouble() * 0.30
        val power = (base * jitter).toInt().coerceIn(MIN_POWER_W, (limitW * 1.1).toInt())
        val voltage = 230 + random.nextInt(-6, 7)
        return PowerReading(power, voltage, currentA = power.toDouble() / voltage)
    }

    /** Споживання від початку доби до [hour]:[minute], кВт·год. */
    fun consumedTodayKwh(hour: Int, minute: Int = 0): Double {
        val h = hour.coerceIn(0, 23)
        val full = hourlyProfileW.take(h).sum()
        val part = hourlyProfileW[h] * (minute.coerceIn(0, 59) / 60.0)
        return (full + part) / 1000.0
    }

    companion object {
        const val DEFAULT_LIMIT_W = 5000
        const val MIN_POWER_W = 150
    }
}
