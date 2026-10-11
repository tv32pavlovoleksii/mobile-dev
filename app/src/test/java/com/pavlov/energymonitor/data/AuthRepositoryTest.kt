package com.pavlov.energymonitor.data

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test

/** Переїхав сюди разом з [AuthRepository] під час переходу на MVVM (ПЗ 7): тести — за місцем проживання класу. */
class AuthRepositoryTest {
    private val repo = AuthRepository(latencyMs = 0)

    @Test fun demoLogin() = runBlocking {
        assertTrue(repo.login("DEMO@energy.ua", "Energy2026!") is AuthResult.Success)
        assertTrue(repo.login("demo@energy.ua", "wrong") is AuthResult.Failure)
        assertTrue(repo.login("nobody@energy.ua", "x") is AuthResult.Failure)
    }

    @Test fun registerThenLogin_andDuplicateRejected() = runBlocking {
        assertTrue(repo.register("Олексій", "a@b.ua", "Pass1234") is AuthResult.Success)
        assertTrue(repo.register("Хтось", "A@b.ua", "Pass1234") is AuthResult.Failure)
        assertTrue(repo.login("a@b.ua", "Pass1234") is AuthResult.Success)
    }
}
