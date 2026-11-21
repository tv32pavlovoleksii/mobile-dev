package com.pavlov.energymonitor.data

import com.pavlov.energymonitor.data.remote.EnergyApiService
import com.pavlov.energymonitor.data.remote.ErrorHandler
import com.pavlov.energymonitor.data.remote.dto.AddReadingRequest
import com.pavlov.energymonitor.data.remote.dto.ReadingDto
import com.pavlov.energymonitor.data.remote.dto.ReadingStatsDto
import com.pavlov.energymonitor.data.remote.dto.TariffDto
import com.pavlov.energymonitor.domain.ConsumptionSimulator
import com.pavlov.energymonitor.domain.PowerReading

/**
 * ПЗ 9 (просунутий рівень): репозиторій моніторингу агрегує два джерела даних —
 * [ConsumptionSimulator] (локальне, миттєве — імітує показник лічильника без мережі) і
 * [EnergyApiService] (віддалене — хмарна статистика та архів показників через REST).
 * Він же виконує кешування останньої вдалої відповіді й бізнес-логіку відправки локального
 * показника в хмару, а [ErrorHandler] (ПЗ 12) — повторні спроби при тимчасових збоях мережі.
 */
class MonitorRepository(
    private val simulator: ConsumptionSimulator,
    private val api: EnergyApiService,
) {
    val limitW: Int get() = simulator.limitW

    fun nextLocalReading(hour: Int): PowerReading = simulator.nextReading(hour)

    fun consumedTodayKwh(hour: Int, minute: Int): Double = simulator.consumedTodayKwh(hour, minute)

    private var cachedStats: ReadingStatsDto? = null

    /**
     * Віддалене джерело: агрегована статистика з хмари. Якщо запит не вдався (сервер тимчасово
     * недоступний), повертає останнє відоме значення з кешу замість того, щоб просто показати
     * помилку, — це і є "кешування та обробка помилок на рівні Repository" з методички.
     */
    suspend fun fetchCloudStats(): Result<ReadingStatsDto> = try {
        val stats = ErrorHandler.withRetry { api.getReadingStats() }
        cachedStats = stats
        Result.success(stats)
    } catch (e: Exception) {
        cachedStats?.let { Result.success(it) } ?: Result.failure(e)
    }

    suspend fun fetchCloudReadings(limit: Int = 10): Result<List<ReadingDto>> =
        runCatching { ErrorHandler.withRetry { api.getReadings(limit) } }

    suspend fun fetchTariffs(): Result<List<TariffDto>> =
        runCatching { ErrorHandler.withRetry { api.getTariffs() } }

    /** Надсилає поточний локальний показник у хмару — приклад запису через Repository (ПЗ 9/10). */
    suspend fun pushReadingToCloud(reading: PowerReading): Result<ReadingDto> = runCatching {
        api.addReading(AddReadingRequest(reading.powerW, reading.voltageV.toDouble(), reading.currentA))
    }

    /** Демонстрація retry-логіки (ПЗ 12): перші спроби навмисно повертають 503 на сервері. */
    suspend fun checkConnectionWithRetry(): Result<String> = runCatching {
        ErrorHandler.withRetry(times = 4) { api.getFlaky() }.message
    }
}
