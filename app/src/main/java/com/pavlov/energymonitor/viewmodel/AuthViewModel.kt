package com.pavlov.energymonitor.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pavlov.energymonitor.data.AuthRepository
import com.pavlov.energymonitor.data.AuthResult
import com.pavlov.energymonitor.domain.AuthValidator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class AuthMode(val title: String) { LOGIN("Вхід"), REGISTER("Реєстрація") }

/** Повідомлення про результат дії: колір залежить від успіху. */
data class AuthMessage(val text: String, val isError: Boolean)

/**
 * Увесь стан екранів входу/реєстрації одним знімком. У ПЗ 4 це був клас `AuthFormState`
 * зі звичайним Compose State; тепер (ПЗ 7) те саме живе в `StateFlow` всередині `ViewModel`,
 * а похідні поля (помилки полів) лишаються обчислюваними властивостями — логіка та сама,
 * змінилося лише місце, де вона живе.
 */
data class AuthUiState(
    val mode: AuthMode = AuthMode.LOGIN,
    val name: String = "",
    val email: String = "",
    val password: String = "",
    val confirm: String = "",
    val accepted: Boolean = false,
    val passwordVisible: Boolean = false,
    val showErrors: Boolean = false,
    val loading: Boolean = false,
    val message: AuthMessage? = null,
    val signedInUser: String? = null,
) {
    private val isRegister get() = mode == AuthMode.REGISTER

    val nameError: String? get() = if (showErrors && isRegister) AuthValidator.validateName(name) else null
    val emailError: String? get() = if (showErrors) AuthValidator.validateEmail(email) else null
    val passwordError: String? get() = when {
        !showErrors -> null
        isRegister -> AuthValidator.validatePassword(password)
        else -> if (password.isEmpty()) "Введіть пароль" else null
    }
    val confirmError: String? get() = if (showErrors && isRegister) AuthValidator.validateConfirm(password, confirm) else null
    val termsError: Boolean get() = showErrors && isRegister && !accepted

    val isValid: Boolean get() {
        val base = AuthValidator.validateEmail(email) == null && password.isNotEmpty()
        return if (isRegister) {
            base && AuthValidator.validateName(name) == null &&
                AuthValidator.validatePassword(password) == null &&
                AuthValidator.validateConfirm(password, confirm) == null && accepted
        } else base
    }
}

/**
 * ПЗ 7 (MVVM): `ViewModel` для ПЗ 4. Мережевий виклик (імітований [AuthRepository])
 * тепер виконується у `viewModelScope`, а не в `rememberCoroutineScope()` екрана,
 * тому результат не губиться, навіть якщо користувач встигне покинути екран.
 */
class AuthViewModel(private val repository: AuthRepository) : ViewModel() {
    private val _state = MutableStateFlow(AuthUiState())
    val state: StateFlow<AuthUiState> = _state.asStateFlow()

    fun switchMode(mode: AuthMode) = _state.update { it.copy(mode = mode, showErrors = false, message = null) }
    fun onNameChange(v: String) = _state.update { it.copy(name = v) }
    fun onEmailChange(v: String) = _state.update { it.copy(email = v) }
    fun onPasswordChange(v: String) = _state.update { it.copy(password = v) }
    fun onConfirmChange(v: String) = _state.update { it.copy(confirm = v) }
    fun onAcceptedChange(v: Boolean) = _state.update { it.copy(accepted = v) }
    fun togglePasswordVisible() = _state.update { it.copy(passwordVisible = !it.passwordVisible) }

    fun signOut() = _state.update {
        it.copy(signedInUser = null, password = "", confirm = "", showErrors = false, message = null)
    }

    fun submit() {
        val current = _state.value
        _state.update { it.copy(showErrors = true) }
        if (!current.isValid || current.loading) return

        viewModelScope.launch {
            _state.update { it.copy(loading = true, message = null) }
            val result = if (current.mode == AuthMode.LOGIN) repository.login(current.email, current.password)
            else repository.register(current.name, current.email, current.password)
            _state.update { s ->
                when (result) {
                    is AuthResult.Success -> s.copy(loading = false, signedInUser = result.userName)
                    is AuthResult.Failure -> s.copy(loading = false, message = AuthMessage(result.message, isError = true))
                }
            }
        }
    }
}
