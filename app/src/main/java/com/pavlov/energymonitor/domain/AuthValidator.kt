package com.pavlov.energymonitor.domain

/** Складність пароля для індикатора. */
enum class PasswordStrength(val title: String, val level: Int) {
    EMPTY("", 0), WEAK("Слабкий", 1), FAIR("Середній", 2), GOOD("Добрий", 3), STRONG("Надійний", 4)
}

/**
 * Валідація полів форм входу та реєстрації (ПЗ 4).
 * Кожен метод повертає текст помилки або null, якщо значення коректне.
 */
object AuthValidator {
    private val EMAIL_REGEX = Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
    const val MIN_PASSWORD_LENGTH = 8

    fun validateName(name: String): String? = when {
        name.isBlank() -> "Введіть ім’я"
        name.trim().length < 2 -> "Ім’я занадто коротке"
        else -> null
    }

    fun validateEmail(email: String): String? = when {
        email.isBlank() -> "Введіть електронну пошту"
        !EMAIL_REGEX.matches(email.trim()) -> "Некоректна адреса пошти"
        else -> null
    }

    fun validatePassword(password: String): String? = when {
        password.isEmpty() -> "Введіть пароль"
        password.length < MIN_PASSWORD_LENGTH -> "Мінімум $MIN_PASSWORD_LENGTH символів"
        !password.any { it.isDigit() } -> "Додайте хоча б одну цифру"
        !password.any { it.isLetter() } -> "Додайте хоча б одну літеру"
        else -> null
    }

    fun validateConfirm(password: String, confirm: String): String? = when {
        confirm.isEmpty() -> "Підтвердіть пароль"
        confirm != password -> "Паролі не збігаються"
        else -> null
    }

    /** Оцінка надійності: довжина, регістри, цифри, спецсимволи. */
    fun strength(password: String): PasswordStrength {
        if (password.isEmpty()) return PasswordStrength.EMPTY
        var score = 0
        if (password.length >= MIN_PASSWORD_LENGTH) score++
        if (password.any { it.isUpperCase() } && password.any { it.isLowerCase() }) score++
        if (password.any { it.isDigit() }) score++
        if (password.any { !it.isLetterOrDigit() }) score++
        return when (score) {
            0, 1 -> PasswordStrength.WEAK
            2 -> PasswordStrength.FAIR
            3 -> PasswordStrength.GOOD
            else -> PasswordStrength.STRONG
        }
    }
}
