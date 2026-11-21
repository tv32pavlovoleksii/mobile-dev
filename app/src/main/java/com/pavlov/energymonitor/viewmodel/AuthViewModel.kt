package com.pavlov.energymonitor.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pavlov.energymonitor.data.AuthRepository
import com.pavlov.energymonitor.data.AuthResult
import com.pavlov.energymonitor.data.remote.ErrorHandler
import com.pavlov.energymonitor.data.remote.Session
import com.pavlov.energymonitor.data.remote.dto.ReadingDto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Повідомлення про результат дії: колір залежить від успіху. */
data class AuthMessage(val text: String, val isError: Boolean)

/**
 * Увесь стан екрана входу одним знімком. [session] приходить з [AuthRepository.session] —
 * він переживає навіть перезапуск процесу (токен лежить у `DataStore`, ПЗ 11), тому
 * користувач лишається залогіненим між запусками застосунку, як у звичайному мобільному клієнті.
 */
data class AuthUiState(
    val username: String = "",
    val password: String = "",
    val passwordVisible: Boolean = false,
    val showErrors: Boolean = false,
    val loading: Boolean = false,
    val message: AuthMessage? = null,
    val session: Session? = null,
    val protectedMessage: String? = null,
    val adminReadings: List<ReadingDto>? = null,
    val secureActionLoading: Boolean = false,
) {
    val usernameError: String? get() = if (showErrors && username.isBlank()) "Введіть логін" else null
    val passwordError: String? get() = if (showErrors && password.isBlank()) "Введіть пароль" else null
    val isValid: Boolean get() = username.isNotBlank() && password.isNotBlank()
}

/**
 * ПЗ 7 (MVVM) + ПЗ 11 (токен-автентифікація): `ViewModel` звертається лише до [AuthRepository] —
 * сама вона не знає ні про Retrofit, ні про `DataStore`. Мережевий виклик логіну виконується
 * у `viewModelScope`, тому результат не губиться, навіть якщо користувач покине екран.
 */
class AuthViewModel(private val repository: AuthRepository) : ViewModel() {
    private val _state = MutableStateFlow(AuthUiState())
    val state: StateFlow<AuthUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            repository.session.collect { session -> _state.update { it.copy(session = session) } }
        }
    }

    fun onUsernameChange(v: String) = _state.update { it.copy(username = v) }
    fun onPasswordChange(v: String) = _state.update { it.copy(password = v) }
    fun togglePasswordVisible() = _state.update { it.copy(passwordVisible = !it.passwordVisible) }

    fun submit() {
        val current = _state.value
        _state.update { it.copy(showErrors = true) }
        if (!current.isValid || current.loading) return

        viewModelScope.launch {
            _state.update { it.copy(loading = true, message = null) }
            when (val result = repository.login(current.username, current.password)) {
                is AuthResult.Success -> _state.update { it.copy(loading = false, password = "") }
                is AuthResult.Failure -> _state.update { it.copy(loading = false, message = AuthMessage(result.message, isError = true)) }
            }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            repository.logout()
            _state.update {
                it.copy(username = "", password = "", showErrors = false, message = null, protectedMessage = null, adminReadings = null)
            }
        }
    }

    /** ПЗ 11/12: запит до захищеного ресурсу; прострочений токен викликає повторний логін. */
    fun loadProtectedData() {
        viewModelScope.launch {
            _state.update { it.copy(secureActionLoading = true) }
            repository.loadProtectedMessage()
                .onSuccess { msg -> _state.update { it.copy(secureActionLoading = false, protectedMessage = msg, message = null) } }
                .onFailure { e -> handleSecureFailure(e) }
        }
    }

    fun loadAdminData() {
        viewModelScope.launch {
            _state.update { it.copy(secureActionLoading = true) }
            repository.loadAdminReadings()
                .onSuccess { list -> _state.update { it.copy(secureActionLoading = false, adminReadings = list, message = null) } }
                .onFailure { e -> handleSecureFailure(e) }
        }
    }

    private suspend fun handleSecureFailure(e: Throwable) {
        val message = ErrorHandler.messageFor(e)
        if (message.contains("авторизація")) repository.logout() // токен прострочено/недійсний — примусовий вихід
        _state.update { it.copy(secureActionLoading = false, message = AuthMessage(message, isError = true)) }
    }
}
