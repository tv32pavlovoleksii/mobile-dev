package com.pavlov.energymonitor.data.remote

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * У звичайному емуляторі Android `10.0.2.2` — псевдонім хост-машини з `server/app.py`.
 * Тут використано `127.0.0.1` разом із `adb reverse tcp:5156 tcp:5156`: емулятор бачить
 * локальний порт самого пристрою, а adb прозоро тунелює його на хост. Це надійніше за
 * `10.0.2.2`, коли файрвол хоста блокує вхідні з'єднання від віртуальної мережі емулятора.
 */
private const val BASE_URL = "http://127.0.0.1:5156/"

/**
 * ПЗ 10: збирає Retrofit-клієнт один раз на застосунок. `AuthInterceptor` (ПЗ 11) підставляє
 * токен у кожен запит, `HttpLoggingInterceptor` — лише для налагодження в Logcat.
 */
fun createEnergyApi(tokenStore: TokenStore): EnergyApiService {
    val logging = HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC }
    val client = OkHttpClient.Builder()
        .addInterceptor(AuthInterceptor(tokenStore))
        .addInterceptor(logging)
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.SECONDS)
        .build()
    return Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(client)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(EnergyApiService::class.java)
}
