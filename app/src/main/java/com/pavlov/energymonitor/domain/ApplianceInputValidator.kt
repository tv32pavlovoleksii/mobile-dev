package com.pavlov.energymonitor.domain

/**
 * Розбір і перевірка введених користувачем параметрів приладу (ПЗ 2).
 * Приймає і крапку, і кому як десятковий роздільник.
 */
object ApplianceInputValidator {
    const val MAX_POWER_W = 20_000

    fun validateName(name: String): String? =
        if (name.isBlank()) "Введіть назву" else null

    /** Повертає (значення, помилка): рівно одне з двох не null. */
    fun parsePower(text: String): Pair<Int?, String?> {
        val value = text.trim().toIntOrNull()
            ?: return null to "Введіть ціле число, Вт"
        return if (value !in 1..MAX_POWER_W) null to "Від 1 до $MAX_POWER_W Вт" else value to null
    }

    fun parseHours(text: String): Pair<Double?, String?> {
        val value = text.trim().replace(',', '.').toDoubleOrNull()
            ?: return null to "Введіть число, год"
        return if (value <= 0.0 || value > 24.0) null to "Від 0,1 до 24 год" else value to null
    }
}
