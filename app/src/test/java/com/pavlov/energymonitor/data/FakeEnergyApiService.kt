package com.pavlov.energymonitor.data

import com.pavlov.energymonitor.data.remote.EnergyApiService
import com.pavlov.energymonitor.data.remote.dto.AddReadingRequest
import com.pavlov.energymonitor.data.remote.dto.LoginRequest
import com.pavlov.energymonitor.data.remote.dto.LoginResponse
import com.pavlov.energymonitor.data.remote.dto.MessageDto
import com.pavlov.energymonitor.data.remote.dto.ReadingDto
import com.pavlov.energymonitor.data.remote.dto.ReadingStatsDto
import com.pavlov.energymonitor.data.remote.dto.TariffDto
import retrofit2.HttpException
import retrofit2.Response

private val USERS = mapOf(
    "demo" to ("Energy2026!" to "USER"),
    "admin" to ("Admin2026!" to "ADMIN"),
)

/** Тестовий дублер [EnergyApiService] — без Retrofit і без реального сервера. */
class FakeEnergyApiService : EnergyApiService {
    var currentRole: String? = null
    var failuresBeforeSuccess = 0
    var lastAddedReading: AddReadingRequest? = null
    var statsShouldFail = false

    override suspend fun getReadings(limit: Int): List<ReadingDto> =
        listOf(ReadingDto(1, 1_000L, 1800, 230.0, 7.8))

    override suspend fun getReadingStats(): ReadingStatsDto {
        if (statsShouldFail) throw java.io.IOException("Немає з'єднання")
        return ReadingStatsDto(4, 1896.2, 2100, 1705)
    }

    override suspend fun addReading(request: AddReadingRequest): ReadingDto {
        lastAddedReading = request
        return ReadingDto(99, 2_000L, request.powerW, request.voltageV, request.currentA)
    }

    override suspend fun getTariffs(): List<TariffDto> =
        listOf(TariffDto(1, "Одна зона", 4.32, 4.32), TariffDto(2, "Дві зони", 4.32, 2.16))

    override suspend fun getFlaky(): MessageDto {
        if (failuresBeforeSuccess > 0) {
            failuresBeforeSuccess--
            throw httpError(503)
        }
        return MessageDto("ok")
    }

    override suspend fun simulateError(code: Int): MessageDto = throw httpError(code)

    override suspend fun login(request: LoginRequest): LoginResponse {
        val entry = USERS[request.username]
        if (entry == null || entry.first != request.password) throw httpError(401)
        currentRole = entry.second
        return LoginResponse(token = "test-token", role = entry.second, expiresInSec = 300)
    }

    override suspend fun getProtected(): MessageDto {
        if (currentRole == null) throw httpError(401)
        return MessageDto("Привіт, роль: $currentRole")
    }

    override suspend fun getAdminReadings(): List<ReadingDto> {
        if (currentRole == null) throw httpError(401)
        if (currentRole != "ADMIN") throw httpError(403)
        return listOf(ReadingDto(1, 1_000L, 1800, 230.0, 7.8))
    }

    private fun httpError(code: Int): HttpException {
        val body = okhttp3.ResponseBody.create(null, "{}")
        return HttpException(Response.error<Any>(code, body))
    }
}
