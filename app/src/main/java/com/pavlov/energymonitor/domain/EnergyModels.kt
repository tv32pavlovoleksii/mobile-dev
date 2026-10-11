package com.pavlov.energymonitor.domain

/**
 * Доменні моделі системи моніторингу енергоспоживання.
 * Файл не залежить від Android/Compose, тому його легко тестувати та повторно використовувати.
 */

/** Одне вимірювання лічильника (симуляція смарт-метра). */
data class PowerReading(
    val powerW: Int,
    val voltageV: Int,
    val currentA: Double,
)

/** Прилад у розрахунку споживання. */
data class Appliance(
    val id: Int,
    val name: String,
    val emoji: String,
    val powerW: Int,
    val hoursPerDay: Double,
    val quantity: Int = 1,
    val enabled: Boolean = true,
)

/** Тарифний план: одно- або двозонний (нічна зона дешевша). */
enum class TariffPlan(val title: String, val dayRate: Double, val nightRate: Double) {
    SINGLE("Одна зона", dayRate = 4.32, nightRate = 4.32),
    TWO_ZONE("Дві зони", dayRate = 4.32, nightRate = 2.16),
}

/** Підсумок розрахунку за період. */
data class EnergySummary(
    val dailyKwh: Double,
    val periodKwh: Double,
    val cost: Double,
    val co2Kg: Double,
    val perApplianceKwh: List<Pair<Appliance, Double>>,
)

/** Рівень навантаження відносно дозволеної потужності (ліміту). */
enum class LoadLevel(val title: String) {
    LOW("Низьке"),
    NORMAL("Нормальне"),
    HIGH("Високе"),
    CRITICAL("Критичне");

    companion object {
        /** Визначає рівень за часткою поточної потужності від ліміту. */
        fun of(powerW: Int, limitW: Int): LoadLevel {
            require(limitW > 0) { "Ліміт потужності має бути додатнім" }
            val ratio = powerW.toDouble() / limitW
            return when {
                ratio < 0.30 -> LOW
                ratio < 0.60 -> NORMAL
                ratio < 0.85 -> HIGH
                else -> CRITICAL
            }
        }
    }
}
