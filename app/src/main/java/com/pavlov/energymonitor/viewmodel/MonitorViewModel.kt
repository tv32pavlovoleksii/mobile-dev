package com.pavlov.energymonitor.viewmodel

import androidx.lifecycle.ViewModel
import com.pavlov.energymonitor.domain.ConsumptionSimulator
import com.pavlov.energymonitor.domain.EnergyCalculator
import com.pavlov.energymonitor.domain.PowerReading
import com.pavlov.energymonitor.domain.TariffPlan
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.Calendar
import java.util.Date

private const val MAX_HISTORY = 12

/** Увесь стан екрана «Моніторинг» одним незмінним знімком — типовий підхід MVVM. */
data class MonitorUiState(
    val reading: PowerReading,
    val history: List<Int>,
    val updatedAt: Date,
    val limitW: Int,
    val todayKwh: Double,
    val todayCost: Double,
)

/**
 * ПЗ 7 (MVVM): `ViewModel` для ПЗ 1. У PW1 цей стан жив прямо в `MonitorScreen` через
 * `remember`/`mutableStateOf` і губився при повороті екрана. Тепер він живе в `ViewModel`,
 * що переживає зміну конфігурації, а екран лише підписується на [state] і викликає [refresh].
 * Домен (формули, симулятор) залишається тим самим — змінився лише те, хто ним керує.
 */
class MonitorViewModel : ViewModel() {
    private val simulator = ConsumptionSimulator()

    private val _state = MutableStateFlow(initialState())
    val state: StateFlow<MonitorUiState> = _state.asStateFlow()

    /** Дія кнопки «Оновити»: нове вимірювання → новий стан → екран перемальовується сам. */
    fun refresh() {
        _state.update { current ->
            val reading = simulator.nextReading(currentHour())
            val history = (current.history + reading.powerW).takeLast(MAX_HISTORY)
            current.copy(reading = reading, history = history, updatedAt = Date(), todayKwh = todayKwh(), todayCost = todayCost())
        }
    }

    private fun initialState(): MonitorUiState {
        val reading = simulator.nextReading(currentHour())
        return MonitorUiState(
            reading = reading,
            history = listOf(reading.powerW),
            updatedAt = Date(),
            limitW = simulator.limitW,
            todayKwh = todayKwh(),
            todayCost = todayCost(),
        )
    }

    private fun todayKwh(): Double {
        val cal = Calendar.getInstance()
        return simulator.consumedTodayKwh(cal[Calendar.HOUR_OF_DAY], cal[Calendar.MINUTE])
    }

    private fun todayCost(): Double = EnergyCalculator.cost(todayKwh(), TariffPlan.SINGLE)

    private fun currentHour(): Int = Calendar.getInstance()[Calendar.HOUR_OF_DAY]
}
