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
import androidx.compose.runtime.getValue
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pavlov.energymonitor.R
import com.pavlov.energymonitor.domain.AuthValidator
import com.pavlov.energymonitor.domain.PasswordStrength
import com.pavlov.energymonitor.ui.components.IosCard
import com.pavlov.energymonitor.ui.components.IosPrimaryButton
import com.pavlov.energymonitor.ui.components.IosTextField
import com.pavlov.energymonitor.ui.components.IosTintedButton
import com.pavlov.energymonitor.ui.components.SegmentedControl
import com.pavlov.energymonitor.ui.theme.energyColors
import com.pavlov.energymonitor.viewmodel.AuthMessage
import com.pavlov.energymonitor.viewmodel.AuthMode
import com.pavlov.energymonitor.viewmodel.AuthUiState
import com.pavlov.energymonitor.viewmodel.AuthViewModel

/**
 * ПЗ 4 (форми входу/реєстрації) + ПЗ 7 (MVVM): екран лише відображає [AuthUiState]
 * і передає події (`onNameChange`, `submit`, …) у [AuthViewModel] — сам нічого не вирішує
 * й не зберігає. UI розбито на composable-функції: [AuthHeader] → [AuthFields] →
 * [PasswordStrengthBar] → [AuthMessageText] → [SubmitButton]. Layout адаптивний:
 * контент центрується й обмежений 480 dp, тож на планшеті форма не розтягується.
 */
@Composable
fun AuthScreen(vm: AuthViewModel, modifier: Modifier = Modifier) {
    val state by vm.state.collectAsStateWithLifecycle()
    val focus = LocalFocusManager.current

    val submit: () -> Unit = {
        focus.clearFocus()
        vm.submit()
    }

    Box(modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
        Column(Modifier.widthIn(max = 480.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            AnimatedContent(state.signedInUser, transitionSpec = { fadeIn() togetherWith fadeOut() }, label = "authState") { user ->
                if (user != null) {
                    WelcomeCard(user, onSignOut = vm::signOut)
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        AuthHeader(state.mode)
                        SegmentedControl(AuthMode.entries.map { it.title }, state.mode.ordinal, { vm.switchMode(AuthMode.entries[it]) })
                        IosCard { AuthFields(state, vm, submit) }
                        AuthMessageText(state.message)
                        SubmitButton(state.mode, state.loading, submit)
                        SwitchModeButton(state.mode) { vm.switchMode(if (state.mode == AuthMode.LOGIN) AuthMode.REGISTER else AuthMode.LOGIN) }
                        if (state.mode == AuthMode.LOGIN) {
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
private fun AuthFields(state: AuthUiState, vm: AuthViewModel, onDone: () -> Unit) {
    val register = state.mode == AuthMode.REGISTER
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        AnimatedVisibility(register) {
            IosTextField(
                state.name, vm::onNameChange, "Ім’я", error = state.nameError,
                leadingIcon = { Icon(Icons.Default.Person, null) },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            )
        }
        IosTextField(
            state.email, vm::onEmailChange, "Електронна пошта", error = state.emailError,
            leadingIcon = { Icon(Icons.Default.Email, null) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
        )
        PasswordField(
            value = state.password, onValueChange = vm::onPasswordChange, label = "Пароль",
            error = state.passwordError, visible = state.passwordVisible,
            onToggleVisible = vm::togglePasswordVisible,
            imeAction = if (register) ImeAction.Next else ImeAction.Done, onDone = onDone,
        )
        AnimatedVisibility(register) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                PasswordStrengthBar(AuthValidator.strength(state.password))
                PasswordField(
                    value = state.confirm, onValueChange = vm::onConfirmChange, label = "Підтвердіть пароль",
                    error = state.confirmError, visible = state.passwordVisible,
                    onToggleVisible = vm::togglePasswordVisible,
                    imeAction = ImeAction.Done, onDone = onDone,
                )
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(state.accepted, vm::onAcceptedChange)
                    Text(
                        "Погоджуюсь з умовами використання",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (state.termsError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
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
