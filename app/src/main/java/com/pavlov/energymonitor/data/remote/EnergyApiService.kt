package com.pavlov.energymonitor.data.remote

import com.pavlov.energymonitor.data.remote.dto.AddReadingRequest
import com.pavlov.energymonitor.data.remote.dto.LoginRequest
import com.pavlov.energymonitor.data.remote.dto.LoginResponse
import com.pavlov.energymonitor.data.remote.dto.MessageDto
import com.pavlov.energymonitor.data.remote.dto.ReadingDto
import com.pavlov.energymonitor.data.remote.dto.ReadingStatsDto
import com.pavlov.energymonitor.data.remote.dto.TariffDto
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * ПЗ 10: опис REST API сервера `server/app.py` у вигляді інтерфейсу. Retrofit сам генерує
 * реалізацію з HTTP-запитів та перетворює JSON у Kotlin-об'єкти через Gson.
 */
interface EnergyApiService {
    @GET("readings")
    suspend fun getReadings(@Query("limit") limit: Int = 20): List<ReadingDto>

    @GET("readings/stats")
    suspend fun getReadingStats(): ReadingStatsDto

    @POST("readings")
    suspend fun addReading(@Body request: AddReadingRequest): ReadingDto

    @GET("tariffs")
    suspend fun getTariffs(): List<TariffDto>

    @GET("readings/flaky")
    suspend fun getFlaky(): MessageDto

    @GET("readings/error/{code}")
    suspend fun simulateError(@Path("code") code: Int): MessageDto

    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): LoginResponse

    @GET("protected")
    suspend fun getProtected(): MessageDto

    @GET("admin/readings")
    suspend fun getAdminReadings(): List<ReadingDto>
}
