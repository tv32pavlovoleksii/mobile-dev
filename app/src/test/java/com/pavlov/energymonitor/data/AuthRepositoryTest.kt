package com.pavlov.energymonitor.data

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** ПЗ 11: `AuthRepository` тестується з фейковим API/сесією — без Retrofit і без Android `Context`. */
class AuthRepositoryTest {
    private val api = FakeEnergyApiService()
    private val sessionStore = FakeSessionStore()
    private val repo = AuthRepository(api, sessionStore)

    @Test fun login_demoUser_succeedsWithRoleUser() = runBlocking {
        val result = repo.login("demo", "Energy2026!")
        assertTrue(result is AuthResult.Success)
        assertEquals("USER", (result as AuthResult.Success).role)
        assertEquals("USER", sessionStore.sessionFlow.first()?.role)
    }

    @Test fun login_wrongPassword_fails() = runBlocking {
        val result = repo.login("demo", "wrong")
        assertTrue(result is AuthResult.Failure)
        assertNull(sessionStore.sessionFlow.first())
    }

    @Test fun logout_clearsSession() = runBlocking {
        repo.login("admin", "Admin2026!")
        repo.logout()
        assertNull(sessionStore.sessionFlow.first())
    }

    @Test fun loadProtectedMessage_requiresLogin() = runBlocking {
        assertTrue(repo.loadProtectedMessage().isFailure)
        repo.login("demo", "Energy2026!")
        assertTrue(repo.loadProtectedMessage().isSuccess)
    }

    @Test fun loadAdminReadings_rejectsNonAdminRole() = runBlocking {
        repo.login("demo", "Energy2026!")
        assertTrue(repo.loadAdminReadings().isFailure) // USER, не ADMIN -> 403

        repo.login("admin", "Admin2026!")
        assertTrue(repo.loadAdminReadings().isSuccess)
    }
}
