package com.pavlov.energymonitor.data.remote

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.authStore by preferencesDataStore(name = "auth_prefs")

/** Знімок збереженої сесії: сам токен, роль користувача, момент спливання (epoch-секунди). */
data class Session(val token: String, val role: String, val expiresAt: Long) {
    val isExpired: Boolean get() = System.currentTimeMillis() / 1000 >= expiresAt
}

/**
 * Абстракція над сховищем сесії — завдяки їй [com.pavlov.energymonitor.data.AuthRepository]
 * можна протестувати з простим фейком у пам'яті, без Android `Context`/`DataStore`.
 */
interface SessionStore {
    val sessionFlow: Flow<Session?>
    suspend fun save(token: String, role: String, ttlSec: Int)
    suspend fun clear()
}

/**
 * ПЗ 11: локальне сховище мокованого токена через Jetpack DataStore — переживає перезапуск
 * процесу застосунку (на відміну від простого поля в `ViewModel`). [AuthInterceptor] читає
 * його синхронно при кожному запиті, щоб підставити заголовок `Authorization`.
 */
class TokenStore(private val context: Context) : SessionStore {
    private val keyToken = stringPreferencesKey("token")
    private val keyRole = stringPreferencesKey("role")
    private val keyExpiresAt = longPreferencesKey("expiresAt")

    override val sessionFlow: Flow<Session?> = context.authStore.data.map { prefs ->
        val token = prefs[keyToken] ?: return@map null
        val role = prefs[keyRole] ?: return@map null
        val expiresAt = prefs[keyExpiresAt] ?: return@map null
        Session(token, role, expiresAt)
    }

    suspend fun current(): Session? = sessionFlow.first()

    override suspend fun save(token: String, role: String, ttlSec: Int) {
        context.authStore.edit { prefs ->
            prefs[keyToken] = token
            prefs[keyRole] = role
            prefs[keyExpiresAt] = System.currentTimeMillis() / 1000 + ttlSec
        }
    }

    override suspend fun clear() {
        context.authStore.edit { it.clear() }
    }
}
