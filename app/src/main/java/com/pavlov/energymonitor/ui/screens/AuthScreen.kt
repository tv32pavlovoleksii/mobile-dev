package com.pavlov.energymonitor.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.pavlov.energymonitor.R
import com.pavlov.energymonitor.domain.AuthRepository
import com.pavlov.energymonitor.domain.AuthResult
import com.pavlov.energymonitor.domain.AuthValidator
import com.pavlov.energymonitor.domain.PasswordStrength
import com.pavlov.energymonitor.ui.components.IosCard
import com.pavlov.energymonitor.ui.components.IosPrimaryButton
import com.pavlov.energymonitor.ui.components.IosTextField
import com.pavlov.energymonitor.ui.components.IosTintedButton
import com.pavlov.energymonitor.ui.components.SegmentedControl
import com.pavlov.energymonitor.ui.theme.energyColors
import kotlinx.coroutines.launch

enum class AuthMode(val title: String) { LOGIN("Вхід"), REGISTER("Реєстрація") }

/** Повідомлення про результат дії: колір залежить від успіху. */
data class AuthMessage(val text: String, val isError: Boolean)

/**
 * Стан форми авторизації (state holder). Усе, що змінюється, — Compose State,
 * тому UI автоматично реагує; перевірка даних делегується [AuthValidator].
 * Помилки показуються після першої спроби надсилання ([showErrors]).
 */
@Stable
class AuthFormState {
    var mode by mutableStateOf(AuthMode.LOGIN)
    var name by mutableStateOf("")
    var email by mutableStateOf("")
    var password by mutableStateOf("")
    var confirm by mutableStateOf("")
    var accepted by mutableStateOf(false)
    var passwordVisible by mutableStateOf(false)
    var showErrors by mutableStateOf(false)
    var loading by mutableStateOf(false)
    var message by mutableStateOf<AuthMessage?>(null)
    var signedInUser by mutableStateOf<String?>(null)

    /** Репозиторій живе разом зі станом, щоб зареєстровані користувачі не зникали при зміні вкладки. */
    val repository = AuthRepository()

    private val isRegister get() = mode == AuthMode.REGISTER

    val nameError get() = if (showErrors && isRegister) AuthValidator.validateName(name) else null
    val emailError get() = if (showErrors) AuthValidator.validateEmail(email) else null
    val passwordError get() = when {
        !showErrors -> null
        isRegister -> AuthValidator.validatePassword(password)
        else -> if (password.isEmpty()) "Введіть пароль" else null // при вході складність не перевіряємо
    }
    val confirmError get() = if (showErrors && isRegister) AuthValidator.validateConfirm(password, confirm) else null
    val termsError get() = showErrors && isRegister && !accepted

    fun isValid(): Boolean {
        val base = AuthValidator.validateEmail(email) == null && password.isNotEmpty()
        return if (isRegister) {
            base && AuthValidator.validateName(name) == null &&
                AuthValidator.validatePassword(password) == null &&
                AuthValidator.validateConfirm(password, confirm) == null && accepted
        } else base
    }

    fun switchMode(new: AuthMode) {
        mode = new
        showErrors = false
        message = null
    }
}

/**
 * ПЗ 4 (просунутий рівень): екрани входу та реєстрації.
 *
 * UI розбито на composable-функції: [AuthHeader] → [AuthFields] → [PasswordStrengthBar] →
 * [AuthMessageText] → [SubmitButton]. Стан живе в [AuthFormState], логіка — у пакеті domain.
 * Layout адаптивний: контент центрується і обмежений 480 dp, тож на планшеті форма не розтягується.
 */
@Composable
fun AuthScreen(form: AuthFormState, modifier: Modifier = Modifier) {
    val scope = rememberCoroutineScope()
    val focus = LocalFocusManager.current

    val submit: () -> Unit = submit@{
        focus.clearFocus()
        form.showErrors = true
        if (!form.isValid() || form.loading) return@submit
        scope.launch {
            form.loading = true
            form.message = null
            val result = if (form.mode == AuthMode.LOGIN) form.repository.login(form.email, form.password)
            else form.repository.register(form.name, form.email, form.password)
            form.loading = false
            when (result) {
                is AuthResult.Success -> form.signedInUser = result.userName
                is AuthResult.Failure -> form.message = AuthMessage(result.message, isError = true)
            }
        }
    }

    Box(modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
        Column(Modifier.widthIn(max = 480.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            AnimatedContent(form.signedInUser, transitionSpec = { fadeIn() togetherWith fadeOut() }, label = "authState") { user ->
                if (user != null) {
                    WelcomeCard(user, onSignOut = {
                        form.signedInUser = null
                        form.password = ""; form.confirm = ""; form.showErrors = false
                        form.message = null
                    })
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        AuthHeader(form.mode)
                        SegmentedControl(AuthMode.entries.map { it.title }, form.mode.ordinal, { form.switchMode(AuthMode.entries[it]) })
                        IosCard { AuthFields(form, submit) }
                        AuthMessageText(form.message)
                        SubmitButton(form.mode, form.loading, submit)
                        SwitchModeButton(form.mode) { form.switchMode(if (form.mode == AuthMode.LOGIN) AuthMode.REGISTER else AuthMode.LOGIN) }
                        if (form.mode == AuthMode.LOGIN) {
                            Text(
                                "Демо-доступ: demo@energy.ua / Energy2026!",
                                Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Center,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.energyColors.secondaryLabel,
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

/** Логотип і заголовок; текст змінюється з анімацією при перемиканні режиму. */
@Composable
private fun AuthHeader(mode: AuthMode) {
    Column(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Image(painterResource(R.drawable.ic_energy_hero), null, Modifier.size(72.dp).clip(CircleShape))
        AnimatedContent(mode, transitionSpec = { fadeIn() togetherWith fadeOut() }, label = "authTitle") {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(top = 12.dp)) {
                Text(
                    if (it == AuthMode.LOGIN) "З поверненням" else "Створити акаунт",
                    style = MaterialTheme.typography.headlineMedium,
                )
                Text(
                    if (it == AuthMode.LOGIN) "Увійдіть, щоб бачити своє енергоспоживання"
                    else "Стежте за електроенергією та економте щомісяця",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.energyColors.secondaryLabel,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

/** Поля форми. Додаткові поля реєстрації з’являються/зникають з анімацією. */
@Composable
private fun AuthFields(form: AuthFormState, onDone: () -> Unit) {
    val register = form.mode == AuthMode.REGISTER
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        AnimatedVisibility(register) {
            IosTextField(
                form.name, { form.name = it }, "Ім’я", error = form.nameError,
                leadingIcon = { Icon(Icons.Default.Person, null) },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            )
        }
        IosTextField(
            form.email, { form.email = it }, "Електронна пошта", error = form.emailError,
            leadingIcon = { Icon(Icons.Default.Email, null) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
        )
        PasswordField(
            value = form.password, onValueChange = { form.password = it }, label = "Пароль",
            error = form.passwordError, visible = form.passwordVisible,
            onToggleVisible = { form.passwordVisible = !form.passwordVisible },
            imeAction = if (register) ImeAction.Next else ImeAction.Done, onDone = onDone,
        )
        AnimatedVisibility(register) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                PasswordStrengthBar(AuthValidator.strength(form.password))
                PasswordField(
                    value = form.confirm, onValueChange = { form.confirm = it }, label = "Підтвердіть пароль",
                    error = form.confirmError, visible = form.passwordVisible,
                    onToggleVisible = { form.passwordVisible = !form.passwordVisible },
                    imeAction = ImeAction.Done, onDone = onDone,
                )
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(form.accepted, { form.accepted = it })
                    Text(
                        "Погоджуюсь з умовами використання",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (form.termsError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }
    }
}

/** Поле пароля з кнопкою «Показати/Сховати» у стилі iOS (текстова кнопка замість іконки). */
@Composable
private fun PasswordField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    error: String?,
    visible: Boolean,
    onToggleVisible: () -> Unit,
    imeAction: ImeAction,
    onDone: () -> Unit,
) {
    IosTextField(
        value, onValueChange, label, error = error,
        leadingIcon = { Icon(Icons.Default.Lock, null) },
        trailingIcon = { TextButton(onToggleVisible) { Text(if (visible) "Сховати" else "Показати") } },
        visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = imeAction),
        keyboardActions = KeyboardActions(onDone = { onDone() }),
    )
}

/** Чотирисегментний індикатор надійності пароля з анімованим кольором. */
@Composable
private fun PasswordStrengthBar(strength: PasswordStrength) {
    val colors = MaterialTheme.energyColors
    val target = when (strength) {
        PasswordStrength.EMPTY -> MaterialTheme.colorScheme.surfaceVariant
        PasswordStrength.WEAK -> colors.danger
        PasswordStrength.FAIR -> colors.warning
        PasswordStrength.GOOD -> MaterialTheme.colorScheme.primary
        PasswordStrength.STRONG -> colors.good
    }
    val color by animateColorAsState(target, label = "strengthColor")
    Column(Modifier.padding(vertical = 4.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            repeat(4) { i ->
                val fill by animateFloatAsState(if (i < strength.level) 1f else 0f, label = "segment$i")
                Box(
                    Modifier.weight(1f).height(4.dp).clip(RoundedCornerShape(2.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                ) { Box(Modifier.fillMaxWidth(fill).height(4.dp).background(color)) }
            }
        }
        Text(
            if (strength == PasswordStrength.EMPTY) "Мінімум ${AuthValidator.MIN_PASSWORD_LENGTH} символів, літери й цифри"
            else "Надійність: ${strength.title.lowercase()}",
            Modifier.padding(top = 4.dp),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.energyColors.secondaryLabel,
        )
    }
}

/** Повідомлення про помилку сервера (червоне); з’являється з анімацією. */
@Composable
private fun AuthMessageText(message: AuthMessage?) {
    AnimatedVisibility(message != null) {
        val m = message
        Text(
            m?.text.orEmpty(),
            Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyMedium,
            color = if (m?.isError == false) MaterialTheme.energyColors.good else MaterialTheme.colorScheme.error,
        )
    }
}

/** Головна кнопка: під час завантаження показує індикатор і блокується. */
@Composable
private fun SubmitButton(mode: AuthMode, loading: Boolean, onClick: () -> Unit) {
    IosPrimaryButton(text = "", onClick = onClick, enabled = !loading) {
        if (loading) {
            CircularProgressIndicator(Modifier.size(22.dp), color = Color.White, strokeWidth = 2.dp)
            Text("Зачекайте…", Modifier.padding(start = 10.dp), style = MaterialTheme.typography.labelLarge, color = Color.White)
        } else {
            Text(if (mode == AuthMode.LOGIN) "Увійти" else "Зареєструватися", style = MaterialTheme.typography.labelLarge, color = Color.White)
        }
    }
}

@Composable
private fun SwitchModeButton(mode: AuthMode, onClick: () -> Unit) {
    TextButton(onClick, Modifier.fillMaxWidth()) {
        Text(if (mode == AuthMode.LOGIN) "Немає акаунта? Зареєструватися" else "Вже є акаунт? Увійти")
    }
}

/** Екран після успішної авторизації. */
@Composable
private fun WelcomeCard(userName: String, onSignOut: () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(top = 24.dp), verticalArrangement = Arrangement.spacedBy(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Image(painterResource(R.drawable.ic_energy_hero), null, Modifier.size(96.dp).clip(CircleShape))
        Text("Вітаємо, $userName!", style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
        Text(
            "Авторизація пройшла успішно. Тепер доступні всі показники вашої мережі.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.energyColors.secondaryLabel,
            textAlign = TextAlign.Center,
        )
        IosTintedButton("Вийти", onSignOut, Modifier.padding(top = 8.dp), tint = MaterialTheme.colorScheme.error)
    }
}
