package com.pavlov.energymonitor.data

import com.pavlov.energymonitor.data.remote.Session
import com.pavlov.energymonitor.data.remote.SessionStore
import kotlinx.coroutines.flow.MutableStateFlow

/** Сесія в пам'яті — без Android `Context`/`DataStore`, для юніт-тестів [AuthRepository]. */
class FakeSessionStore : SessionStore {
    private val state = MutableStateFlow<Session?>(null)
    override val sessionFlow = state

    override suspend fun save(token: String, role: String, ttlSec: Int) {
        state.value = Session(token, role, System.currentTimeMillis() / 1000 + ttlSec)
    }

    override suspend fun clear() {
        state.value = null
    }
}
