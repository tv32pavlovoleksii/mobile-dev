package com.pavlov.energymonitor.data

import com.pavlov.energymonitor.data.remote.EnergyApiService
import com.pavlov.energymonitor.data.remote.ErrorHandler
import com.pavlov.energymonitor.data.remote.Session
import com.pavlov.energymonitor.data.remote.SessionStore
import com.pavlov.energymonitor.data.remote.dto.LoginRequest
import com.pavlov.energymonitor.data.remote.dto.ReadingDto
import kotlinx.coroutines.flow.Flow
import retrofit2.HttpException

/** Результат спроби увійти. */
sealed interface AuthResult {
    data class Success(val role: String) : AuthResult
    data class Failure(val message: String) : AuthResult
}

/**
 * ПЗ 11: реальна (хоч і навчальна) токен-автентифікація через REST API замість локального
 * порівняння хешів із ПЗ 4. `AuthRepository` ховає від `ViewModel` деталі: що токен виданий
 * сервером, має строк дії і зберігається в [TokenStore] між запусками застосунку.
 * Тестові акаунти: demo / Energy2026! (роль USER) та admin / Admin2026! (роль ADMIN).
 */
class AuthRepository(private val api: EnergyApiService, private val sessionStore: SessionStore) {
    val session: Flow<Session?> = sessionStore.sessionFlow

    suspend fun login(username: String, password: String): AuthResult = try {
        val response = api.login(LoginRequest(username.trim(), password))
        sessionStore.save(response.token, response.role, response.expiresInSec)
        AuthResult.Success(response.role)
    } catch (e: HttpException) {
        AuthResult.Failure(if (e.code() == 401) "Невірний логін або пароль" else "Помилка сервера: ${e.code()}")
    } catch (e: Exception) {
        AuthResult.Failure("Немає з'єднання з сервером")
    }

    suspend fun logout() = sessionStore.clear()

    /** ПЗ 11 (просунутий рівень): ресурс, доступний лише авторизованому користувачу. */
    suspend fun loadProtectedMessage(): Result<String> =
        runCatching { ErrorHandler.withRetry { api.getProtected() }.message }

    /** Доступно лише ролі ADMIN — сервер поверне 403 для звичайного користувача. */
    suspend fun loadAdminReadings(): Result<List<ReadingDto>> =
        runCatching { ErrorHandler.withRetry { api.getAdminReadings() } }
}
