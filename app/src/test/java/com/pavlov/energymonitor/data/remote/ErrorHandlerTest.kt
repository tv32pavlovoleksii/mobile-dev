package com.pavlov.energymonitor.data.remote

import kotlinx.coroutines.runBlocking
import okhttp3.ResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException

private fun httpError(code: Int) = HttpException(Response.error<Any>(code, ResponseBody.create(null, "{}")))

/** ПЗ 12: `ErrorHandler` — єдине місце, що вирішує, яку помилку можна повторити, а яку ні. */
class ErrorHandlerTest {
    @Test fun messageFor_mapsKnownHttpCodes() {
        assertEquals("Дані не знайдено", ErrorHandler.messageFor(httpError(404)))
        assertEquals("Внутрішня помилка сервера, спробуйте пізніше", ErrorHandler.messageFor(httpError(500)))
        assertEquals("Немає з'єднання з сервером", ErrorHandler.messageFor(IOException()))
    }

    @Test fun isRetryable_onlyForTransientFailures() {
        assertTrue(ErrorHandler.isRetryable(httpError(503)))
        assertTrue(ErrorHandler.isRetryable(IOException()))
        assertFalse(ErrorHandler.isRetryable(httpError(404)))
        assertFalse(ErrorHandler.isRetryable(httpError(401)))
    }

    @Test fun withRetry_succeedsAfterTransientFailures() = runBlocking {
        var attempts = 0
        val result = ErrorHandler.withRetry(times = 3, delayMs = 1) {
            attempts++
            if (attempts < 3) throw httpError(503)
            "ok"
        }
        assertEquals("ok", result)
        assertEquals(3, attempts)
    }

    @Test(expected = HttpException::class)
    fun withRetry_doesNotRetryNonTransientFailure(): Unit = runBlocking {
        var attempts = 0
        ErrorHandler.withRetry(times = 3, delayMs = 1) {
            attempts++
            throw httpError(404)
        }
        assertEquals(1, attempts)
    }
}
