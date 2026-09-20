package com.pavlov.energymonitor.domain

import kotlinx.coroutines.delay
import java.security.MessageDigest

/** Результат операції авторизації/реєстрації. */
sealed interface AuthResult {
    data class Success(val userName: String) : AuthResult
    data class Failure(val message: String) : AuthResult
}

/**
 * Навчальний in-memory «бекенд» з імітацією мережевої затримки.
 * Паролі зберігаються лише як SHA-256 хеші. Демо-акаунт: demo@energy.ua / Energy2026!
 * Для реального застосунку замінюється на мережевий репозиторій з тим самим контрактом.
 */
class AuthRepository(private val latencyMs: Long = 1200) {
    private data class User(val name: String, val passwordHash: String)

    private val users = mutableMapOf(
        "demo@energy.ua" to User("Демо користувач", hash("Energy2026!")),
    )

    suspend fun login(email: String, password: String): AuthResult {
        delay(latencyMs)
        val user = users[email.trim().lowercase()]
            ?: return AuthResult.Failure("Користувача з такою поштою не знайдено")
        return if (user.passwordHash == hash(password)) AuthResult.Success(user.name)
        else AuthResult.Failure("Невірний пароль")
    }

    suspend fun register(name: String, email: String, password: String): AuthResult {
        delay(latencyMs)
        val key = email.trim().lowercase()
        if (key in users) return AuthResult.Failure("Ця пошта вже зареєстрована")
        users[key] = User(name.trim(), hash(password))
        return AuthResult.Success(name.trim())
    }

    private fun hash(value: String): String =
        MessageDigest.getInstance("SHA-256").digest(value.toByteArray())
            .joinToString("") { "%02x".format(it) }
}
