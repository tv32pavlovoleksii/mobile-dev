package com.pavlov.energymonitor.data.remote.dto

/**
 * ПЗ 10: моделі, що точно відображають структуру JSON-відповідей сервера (`server/app.py`).
 * Навмисно окремі від доменних моделей ([com.pavlov.energymonitor.domain.PowerReading] тощо) —
 * формат мережевої відповіді може змінюватись незалежно від того, як дані показує UI.
 */
data class ReadingDto(
    val id: Int,
    val timestamp: Long,
    val powerW: Int,
    val voltageV: Double,
    val currentA: Double,
)

data class ReadingStatsDto(
    val count: Int,
    val avgPowerW: Double,
    val maxPowerW: Int,
    val minPowerW: Int,
)

data class TariffDto(
    val id: Int,
    val name: String,
    val dayRateUah: Double,
    val nightRateUah: Double,
)

data class AddReadingRequest(val powerW: Int, val voltageV: Double, val currentA: Double)

data class LoginRequest(val username: String, val password: String)

data class LoginResponse(val token: String, val role: String, val expiresInSec: Int)

data class MessageDto(val message: String)
