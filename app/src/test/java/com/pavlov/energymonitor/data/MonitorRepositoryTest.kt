package com.pavlov.energymonitor.data

import com.pavlov.energymonitor.domain.ConsumptionSimulator
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

/** ПЗ 9: `MonitorRepository` поєднує локальний симулятор і фейкове віддалене джерело. */
class MonitorRepositoryTest {
    private val api = FakeEnergyApiService()
    private val repo = MonitorRepository(ConsumptionSimulator(Random(1)), api)

    @Test fun nextLocalReading_doesNotTouchNetwork() {
        val reading = repo.nextLocalReading(hour = 18)
        assertTrue(reading.powerW > 0)
    }

    @Test fun fetchCloudStats_cachesLastSuccessForFallback() = runBlocking {
        val first = repo.fetchCloudStats()
        assertTrue(first.isSuccess)

        api.statsShouldFail = true
        val second = repo.fetchCloudStats()
        // Мережа "впала", але Repository повертає останнє відоме значення з кешу, а не помилку.
        assertTrue(second.isSuccess)
        assertEquals(first.getOrNull(), second.getOrNull())
    }

    @Test fun pushReadingToCloud_sendsCurrentReading() = runBlocking {
        val reading = repo.nextLocalReading(hour = 12)
        val result = repo.pushReadingToCloud(reading)
        assertTrue(result.isSuccess)
        assertEquals(reading.powerW, api.lastAddedReading?.powerW)
    }

    @Test fun checkConnectionWithRetry_succeedsAfterTransientFailures() = runBlocking {
        api.failuresBeforeSuccess = 2
        val result = repo.checkConnectionWithRetry()
        assertTrue(result.isSuccess)
    }
}
