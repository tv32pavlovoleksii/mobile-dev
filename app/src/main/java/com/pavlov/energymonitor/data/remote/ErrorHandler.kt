package com.pavlov.energymonitor.data.remote

import kotlinx.coroutines.delay
import retrofit2.HttpException
import java.io.IOException

/**
 * ПЗ 12: централізована обробка мережевих помилок. `ViewModel` ніколи не розбирає
 * `HttpException`/`IOException` сама — вона лише показує те, що тут порахували,
 * тому UI й логіка обробки помилок повністю розділені.
 */
object ErrorHandler {
    fun messageFor(e: Throwable): String = when (e) {
        is HttpException -> when (e.code()) {
            400 -> "Некоректний запит"
            401 -> "Потрібна повторна авторизація"
            403 -> "Доступ заборонено"
            404 -> "Дані не знайдено"
            500 -> "Внутрішня помилка сервера, спробуйте пізніше"
            503 -> "Сервіс тимчасово недоступний"
            else -> "Помилка сервера: ${e.code()}"
        }
        is IOException -> "Немає з'єднання з сервером"
        else -> "Помилка: ${e.message ?: e::class.simpleName}"
    }

    /** true для помилок, які варто повторити автоматично (тимчасові збої мережі/сервера). */
    fun isRetryable(e: Throwable): Boolean =
        e is IOException || (e is HttpException && e.code() in intArrayOf(500, 503))

    /**
     * Виконує [block] повторно (до [times] разів) із затримкою [delayMs], якщо помилка тимчасова.
     * Нетимчасові помилки (400/401/403/404) прокидаються одразу — повторювати їх немає сенсу.
     */
    suspend fun <T> withRetry(times: Int = 3, delayMs: Long = 800, block: suspend () -> T): T {
        var lastError: Throwable? = null
        repeat(times) { attempt ->
            try {
                return block()
            } catch (e: Throwable) {
                lastError = e
                if (!isRetryable(e) || attempt == times - 1) throw e
                delay(delayMs)
            }
        }
        throw lastError ?: IllegalStateException("unreachable")
    }
}
