package com.pavlov.energymonitor.domain

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlin.random.Random

/** Кидається «холодним» потоком датчика, коли він імітує тимчасову втрату зв'язку. */
class SensorTimeoutException(message: String) : Exception(message)

/**
 * ПЗ 13: два незалежні джерела даних мережі моніторингу — миттєвий датчик і повільніша
 * прогнозна модель. Обидва — «холодні» потоки: кожен виклик [sensorFlow]/[forecastFlow]
 * запускає власну симуляцію з нуля, що демонструють basic/medium-рівні; поєднання двох
 * потоків у `combine` та перетворення на «гарячий» `StateFlow` відбувається вже у ViewModel.
 */
class NetworkSignalSimulator(private val random: Random = Random.Default) {

    /** Миттєве навантаження мережі, Вт. Раз у [periodMs] імітує збій датчика. */
    fun sensorFlow(nominalW: Int, periodMs: Long = 1200): Flow<Int> = flow {
        while (true) {
            delay(periodMs)
            if (random.nextInt(8) == 0) throw SensorTimeoutException("Датчик не відповів вчасно")
            val jitter = 0.85 + random.nextDouble() * 0.30
            emit((nominalW * jitter).toInt())
        }
    }

    /** Прогнозоване навантаження, Вт: повільніше змінюється, без збоїв зв'язку. */
    fun forecastFlow(nominalW: Int, periodMs: Long = 1800): Flow<Int> = flow {
        var trend = nominalW.toDouble()
        while (true) {
            delay(periodMs)
            trend += (random.nextDouble() - 0.5) * nominalW * 0.05
            emit(trend.toInt())
        }
    }
}
