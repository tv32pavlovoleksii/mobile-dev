package com.pavlov.energymonitor.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pavlov.energymonitor.data.MonitorRepository
import com.pavlov.energymonitor.data.remote.ErrorHandler
import com.pavlov.energymonitor.data.remote.dto.ReadingStatsDto
import com.pavlov.energymonitor.domain.EnergyCalculator
import com.pavlov.energymonitor.domain.PowerReading
import com.pavlov.energymonitor.domain.TariffPlan
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Date

private const val MAX_HISTORY = 12

/** Стан хмарної (віддаленої) частини екрана — окремо від локального показника нижче. */
sealed interface CloudState {
    data object Idle : CloudState
    data object Loading : CloudState
    data class Loaded(val stats: ReadingStatsDto) : CloudState
    data class Info(val message: String) : CloudState
    data class Error(val message: String) : CloudState
}

/** Увесь стан екрана «Моніторинг» одним незмінним знімком — типовий підхід MVVM. */
data class MonitorUiState(
    val reading: PowerReading,
    val history: List<Int>,
    val updatedAt: Date,
    val limitW: Int,
    val todayKwh: Double,
    val todayCost: Double,
    val cloud: CloudState = CloudState.Idle,
)

/**
 * ПЗ 7 (MVVM) + ПЗ 9/10/12: `ViewModel` для ПЗ 1, тепер через [MonitorRepository] — локальний
 * показник, як і раніше, рахує `ConsumptionSimulator` миттєво, а хмарна статистика (ПЗ 9/10)
 * приходить мережею, з retry при тимчасових збоях і зрозумілим повідомленням при помилці (ПЗ 12).
 */
class MonitorViewModel(private val repository: MonitorRepository) : ViewModel() {
    private val _state = MutableStateFlow(initialState())
    val state: StateFlow<MonitorUiState> = _state.asStateFlow()

    /** Дія кнопки «Оновити»: нове вимірювання → новий стан → екран перемальовується сам. */
    fun refresh() {
        _state.update { current ->
            val reading = repository.nextLocalReading(currentHour())
            val history = (current.history + reading.powerW).takeLast(MAX_HISTORY)
            current.copy(reading = reading, history = history, updatedAt = Date(), todayKwh = todayKwh(), todayCost = todayCost())
        }
    }

    /** ПЗ 9/10: завантажує агреговану статистику з хмари (з кешуванням і retry в Repository). */
    fun loadCloudStats() {
        _state.update { it.copy(cloud = CloudState.Loading) }
        viewModelScope.launch {
            repository.fetchCloudStats()
                .onSuccess { stats -> _state.update { it.copy(cloud = CloudState.Loaded(stats)) } }
                .onFailure { e -> _state.update { it.copy(cloud = CloudState.Error(ErrorHandler.messageFor(e))) } }
        }
    }

    /** Надсилає поточний локальний показник у хмару — запис через Repository (ПЗ 9/10). */
    fun pushCurrentReadingToCloud() {
        viewModelScope.launch {
            repository.pushReadingToCloud(_state.value.reading)
                .onSuccess { loadCloudStats() }
                .onFailure { e -> _state.update { it.copy(cloud = CloudState.Error(ErrorHandler.messageFor(e))) } }
        }
    }

    /** ПЗ 12: демонструє, що тимчасова помилка (503) не завершує роботу — Repository повторює запит сам. */
    fun checkConnection() {
        _state.update { it.copy(cloud = CloudState.Loading) }
        viewModelScope.launch {
            repository.checkConnectionWithRetry()
                .onSuccess { msg -> _state.update { it.copy(cloud = CloudState.Info(msg)) } }
                .onFailure { e -> _state.update { it.copy(cloud = CloudState.Error(ErrorHandler.messageFor(e))) } }
        }
    }

    private fun initialState(): MonitorUiState {
        val reading = repository.nextLocalReading(currentHour())
        return MonitorUiState(
            reading = reading,
            history = listOf(reading.powerW),
            updatedAt = Date(),
            limitW = repository.limitW,
            todayKwh = todayKwh(),
            todayCost = todayCost(),
        )
    }

    private fun todayKwh(): Double {
        val cal = Calendar.getInstance()
        return repository.consumedTodayKwh(cal[Calendar.HOUR_OF_DAY], cal[Calendar.MINUTE])
    }

    private fun todayCost(): Double = EnergyCalculator.cost(todayKwh(), TariffPlan.SINGLE)

    private fun currentHour(): Int = Calendar.getInstance()[Calendar.HOUR_OF_DAY]
}
