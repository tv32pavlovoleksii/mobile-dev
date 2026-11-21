package com.pavlov.energymonitor.domain

/**
 * Режим споживання будинку. [factor] масштабує типовий добовий профіль,
 * [advice] — коротка порада для користувача (ПЗ 3).
 */
enum class EnergyMode(val title: String, val factor: Double, val advice: String) {
    ECO("Економ", 0.70, "Вимкніть прилади в режимі очікування та використовуйте нічний тариф для прання."),
    STANDARD("Стандарт", 1.00, "Споживання в межах норми. Розподіляйте потужні прилади по часу."),
    PEAK("Пік", 1.35, "Увага: не вмикайте одночасно чайник, кондиціонер і духовку — ризик перевищення ліміту."),
}

object ModeForecast {
    /** Прогноз добового споживання, кВт·год, для профілю [profileW] у режимі [mode]. */
    fun dailyKwh(profileW: List<Int>, mode: EnergyMode): Double = profileW.sum() * mode.factor / 1000.0

    /** Прогноз максимальної потужності за добу, Вт. */
    fun peakW(profileW: List<Int>, mode: EnergyMode): Int = ((profileW.maxOrNull() ?: 0) * mode.factor).toInt()
}
