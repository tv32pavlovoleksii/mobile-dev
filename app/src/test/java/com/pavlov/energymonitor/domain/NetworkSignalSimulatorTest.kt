package com.pavlov.energymonitor.domain

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.retry
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

/**
 * ПЗ 13: [NetworkSignalSimulator] віддає нескінченні «холодні» потоки, тому в тестах їх
 * обмежують вручну (`CancellationException` після N значень) — так само, як продакшн-код
 * обмежує їх часом життя `ViewModel`. `retry` страхує від зрідка кидання [SensorTimeoutException].
 */
class NetworkSignalSimulatorTest {
    @Test fun sensorFlow_emitsPositivePowerValues() = runTest {
        val simulator = NetworkSignalSimulator(Random(7))
        val collected = mutableListOf<Int>()
        try {
            simulator.sensorFlow(nominalW = 10_000, periodMs = 1)
                .retry(20) { it is SensorTimeoutException }
                .collect { value ->
                    collected += value
                    if (collected.size == 5) throw CancellationException("тест зібрав достатньо значень")
                }
        } catch (e: CancellationException) {
            // очікувано — так само, як ViewModel просто перестає збирати потік при знищенні.
        }
        assertEquals(5, collected.size)
        collected.forEach { assertTrue(it > 0) }
    }

    @Test fun forecastFlow_staysCloseToNominal() = runTest {
        val simulator = NetworkSignalSimulator(Random(11))
        val collected = mutableListOf<Int>()
        try {
            simulator.forecastFlow(nominalW = 10_000, periodMs = 1).collect { value ->
                collected += value
                if (collected.size == 5) throw CancellationException("тест зібрав достатньо значень")
            }
        } catch (e: CancellationException) {
        }
        assertEquals(5, collected.size)
        // Прогноз змінюється повільно (±5% за крок), тому лишається у розумних межах номіналу.
        collected.forEach { assertTrue(it in 5_000..15_000) }
    }
}
