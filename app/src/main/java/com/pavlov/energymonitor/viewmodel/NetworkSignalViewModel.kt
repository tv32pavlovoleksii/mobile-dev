package com.pavlov.energymonitor.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pavlov.energymonitor.domain.NetworkSignalSimulator
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.retry
import kotlinx.coroutines.flow.retryWhen
import kotlinx.coroutines.flow.stateIn

/** Базовий рівень (ПЗ 13): необроблене останнє значення «холодного» потоку датчика. */
sealed interface RawSignal {
    data object Loading : RawSignal
    data class Value(val watts: Int) : RawSignal
}

/** Середній рівень: значення після трансформації потоку, або повідомлення про збій. */
sealed interface SmoothedSignal {
    data object Loading : SmoothedSignal
    data class Value(val kilowatts: Double) : SmoothedSignal
    data class Failed(val message: String) : SmoothedSignal
}

/** Просунутий рівень: об'єднаний сигнал двох джерел (датчик + прогнозна модель). */
sealed interface CombinedSignal {
    data object Loading : CombinedSignal
    data class Value(val sensorW: Int, val forecastW: Int, val deviationPct: Double) : CombinedSignal
    data class Failed(val message: String) : CombinedSignal
}

private const val NOMINAL_NETWORK_LOAD_W = 180_000

/**
 * ПЗ 13: три `StateFlow`, що прогресивно демонструють роботу з Flow — від простого
 * «холодного» потоку (базовий рівень) до трансформацій з обробкою помилок (середній)
 * і комбінування двох джерел у «гарячий» потік з автоповтором (просунутий).
 */
@OptIn(FlowPreview::class)
class NetworkSignalViewModel(private val simulator: NetworkSignalSimulator = NetworkSignalSimulator()) : ViewModel() {

    /**
     * Базовий рівень: збирає «холодний» потік без трансформацій. [retry] тут не показує
     * користувачу помилку — лише мовчки перезапускає потік, щоб живий тікер не зупинявся назавжди.
     */
    val rawSignal: StateFlow<RawSignal> = simulator.sensorFlow(NOMINAL_NETWORK_LOAD_W)
        .retry { it is com.pavlov.energymonitor.domain.SensorTimeoutException }
        .map<Int, RawSignal> { RawSignal.Value(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RawSignal.Loading)

    /**
     * Середній рівень: `map` (Вт → кВт), `filter` (відкидає фізично неможливі викиди),
     * `debounce` (згладжує надто часті зміни) і `retryWhen`, який — на відміну від `catch` —
     * дозволяє показати користувачу повідомлення про збій і одразу ж перезапустити збір даних.
     */
    val smoothedSignal: StateFlow<SmoothedSignal> = simulator.sensorFlow(NOMINAL_NETWORK_LOAD_W)
        .map { it / 1000.0 }
        .filter { it in 0.0..500.0 }
        .debounce(300)
        .map<Double, SmoothedSignal> { SmoothedSignal.Value(it) }
        .retryWhen { cause, _ ->
            emit(SmoothedSignal.Failed(cause.message ?: "Помилка датчика"))
            delay(500)
            true
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SmoothedSignal.Loading)

    /**
     * Просунутий рівень: `combine` поєднує датчик і прогнозну модель в один сигнал;
     * `retry` на датчику переживає поодинокі збої (до 3 спроб), а зовнішній `retryWhen`
     * перезапускає весь комбінований потік, якщо збоїв забагато — тож показ помилки
     * ніколи не стає остаточним. `SharingStarted.Eagerly` робить потік «гарячим» — він
     * генерує значення весь час життя `ViewModel`, незалежно від підписників на екрані.
     */
    val combinedSignal: StateFlow<CombinedSignal> = combine(
        simulator.sensorFlow(NOMINAL_NETWORK_LOAD_W).retry(retries = 3) { e ->
            val retryable = e is com.pavlov.energymonitor.domain.SensorTimeoutException
            if (retryable) delay(400)
            retryable
        },
        simulator.forecastFlow(NOMINAL_NETWORK_LOAD_W),
    ) { sensorW, forecastW ->
        val deviation = (sensorW - forecastW) * 100.0 / forecastW
        CombinedSignal.Value(sensorW, forecastW, deviation) as CombinedSignal
    }
        .retryWhen { cause, _ ->
            // Після вичерпання 3 спроб датчика показуємо причину й перезапускаємо весь
            // комбінований потік заново - «гарячий» потік не має зависати назавжди.
            emit(CombinedSignal.Failed(cause.message ?: "Потік зупинився після вичерпання спроб"))
            delay(1_500)
            true
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, CombinedSignal.Loading)
}
