package com.pavlov.energymonitor.data.remote

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response

/**
 * ПЗ 11: перехоплювач Retrofit/OkHttp — автоматично додає заголовок `Authorization: Bearer <token>`
 * до кожного запиту, якщо є збережена сесія. Завдяки цьому `EnergyApiService` не має жодного
 * параметра для токена: авторизація підставляється прозоро, в одному місці.
 */
class AuthInterceptor(private val tokenStore: TokenStore) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val session = runBlocking { tokenStore.sessionFlow.first() }
        val request = if (session != null) {
            chain.request().newBuilder().header("Authorization", "Bearer ${session.token}").build()
        } else {
            chain.request()
        }
        return chain.proceed(request)
    }
}
