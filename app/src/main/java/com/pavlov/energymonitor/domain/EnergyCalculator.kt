package com.pavlov.energymonitor.domain

/**
 * Обчислення споживання, вартості та вуглецевого сліду.
 * Усі функції чисті (без побічних ефектів) — UI лише викликає їх зі свого стану.
 */
object EnergyCalculator {
    /** Орієнтовний викид CO₂ на 1 кВт·год для енергосистеми України (кг). */
    const val CO2_KG_PER_KWH = 0.36

    /** Кількість днів у розрахунковому місяці. */
    const val DAYS_IN_MONTH = 30

    /** Добове споживання, кВт·год: P[Вт] × t[год] × N / 1000. */
    fun dailyKwh(powerW: Int, hoursPerDay: Double, quantity: Int = 1): Double {
        val p = powerW.coerceAtLeast(0)
        val h = hoursPerDay.coerceIn(0.0, 24.0)
        val n = quantity.coerceAtLeast(0)
        return p * h * n / 1000.0
    }

    /**
     * Вартість, грн. [nightShare] — частка споживання в нічній зоні (0..1);
     * для однозонного тарифу нічна ставка дорівнює денній.
     */
    fun cost(kwh: Double, plan: TariffPlan, nightShare: Double = 0.3): Double {
        val share = nightShare.coerceIn(0.0, 1.0)
        val rate = plan.dayRate * (1 - share) + plan.nightRate * share
        return kwh * rate
    }

    fun co2Kg(kwh: Double): Double = kwh * CO2_KG_PER_KWH

    /** Зведений розрахунок для списку приладів (вимкнені пропускаються). */
    fun summarize(
        appliances: List<Appliance>,
        plan: TariffPlan,
        nightShare: Double = 0.3,
        days: Int = DAYS_IN_MONTH,
    ): EnergySummary {
        val active = appliances.filter { it.enabled }
        val perAppliance = active.map { it to dailyKwh(it.powerW, it.hoursPerDay, it.quantity) * days }
        val daily = active.sumOf { dailyKwh(it.powerW, it.hoursPerDay, it.quantity) }
        val period = daily * days
        return EnergySummary(
            dailyKwh = daily,
            periodKwh = period,
            cost = cost(period, plan, nightShare),
            co2Kg = co2Kg(period),
            perApplianceKwh = perAppliance,
        )
    }
}
