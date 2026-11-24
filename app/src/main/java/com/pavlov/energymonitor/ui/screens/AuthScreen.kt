package com.pavlov.energymonitor.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pavlov.energymonitor.R
import com.pavlov.energymonitor.data.remote.Session
import com.pavlov.energymonitor.ui.components.IosCard
import com.pavlov.energymonitor.ui.components.IosPrimaryButton
import com.pavlov.energymonitor.ui.components.IosTextField
import com.pavlov.energymonitor.ui.components.IosTintedButton
import com.pavlov.energymonitor.ui.components.StatusChip
import com.pavlov.energymonitor.ui.theme.energyColors
import com.pavlov.energymonitor.viewmodel.AuthUiState
import com.pavlov.energymonitor.viewmodel.AuthViewModel

/**
 * ПЗ 11: екран входу тепер — тонка проєкція [AuthUiState]. Форма входу (немає токена) і
 * «кабінет» (токен є, показує роль і кнопки захищених запитів) — це просто два варіанти
 * одного й того самого `when`, а не два різні екрани.
 */
@Composable
fun AuthScreen(vm: AuthViewModel, modifier: Modifier = Modifier) {
    val state by vm.state.collectAsStateWithLifecycle()
    val focus = LocalFocusManager.current
    val submit: () -> Unit = { focus.clearFocus(); vm.submit() }

    Box(modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
        Column(Modifier.widthIn(max = 480.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            AnimatedContent(state.session, transitionSpec = { fadeIn() togetherWith fadeOut() }, label = "authState") { session ->
                if (session == null) LoginForm(state, vm, submit) else AccountPanel(session, state, vm)
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun LoginForm(state: AuthUiState, vm: AuthViewModel, onDone: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Image(painterResource(R.drawable.ic_energy_hero), null, Modifier.size(72.dp).clip(CircleShape))
            Text("З поверненням", Modifier.padding(top = 12.dp), style = MaterialTheme.typography.headlineMedium)
            Text(
                "Увійдіть, щоб бачити своє енергоспоживання",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.energyColors.secondaryLabel,
                textAlign = TextAlign.Center,
            )
        }
        IosCard {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                IosTextField(
                    state.username, vm::onUsernameChange, "Логін", error = state.usernameError,
                    leadingIcon = { Icon(Icons.Default.Person, null) },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                )
                IosTextField(
                    state.password, vm::onPasswordChange, "Пароль", error = state.passwordError,
                    leadingIcon = { Icon(Icons.Default.Lock, null) },
                    trailingIcon = { TextButton(vm::togglePasswordVisible) { Text(if (state.passwordVisible) "Сховати" else "Показати") } },
                    visualTransformation = if (state.passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardActions = KeyboardActions(onDone = { onDone() }),
                )
            }
        }
        AnimatedVisibility(state.message != null) {
            Text(
                state.message?.text.orEmpty(), Modifier.fillMaxWidth(), textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error,
            )
        }
        IosPrimaryButton(text = "", onClick = onDone, enabled = !state.loading) {
            if (state.loading) {
                CircularProgressIndicator(Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                Text("Зачекайте…", Modifier.padding(start = 8.dp), style = MaterialTheme.typography.labelLarge, color = Color.White)
            } else {
                Text("Увійти", style = MaterialTheme.typography.labelLarge, color = Color.White)
            }
        }
        Text(
            "Тестові акаунти: demo / Energy2026! (USER) · admin / Admin2026! (ADMIN)",
            Modifier.fillMaxWidth(), textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.energyColors.secondaryLabel,
        )
    }
}

@Composable
private fun AccountPanel(session: Session, state: AuthUiState, vm: AuthViewModel) {
    Column(Modifier.fillMaxWidth().padding(top = 24.dp), verticalArrangement = Arrangement.spacedBy(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Image(painterResource(R.drawable.ic_energy_hero), null, Modifier.size(96.dp).clip(CircleShape))
        Text("Вітаємо, ${state.username.ifBlank { "користувачу" }}!", style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
        StatusChip("Роль: ${session.role}", if (session.role == "ADMIN") MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary)

        IosCard {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Захищені дані (ПЗ 11)", style = MaterialTheme.typography.labelLarge)
                IosTintedButton("Завантажити захищене повідомлення", vm::loadProtectedData, Modifier.fillMaxWidth())
                state.protectedMessage?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
                if (session.role == "ADMIN") {
                    IosTintedButton("Завантажити дані адміністратора", vm::loadAdminData, Modifier.fillMaxWidth())
                    state.adminReadings?.let { list ->
                        Text("Показників у базі: ${list.size}", style = MaterialTheme.typography.bodyMedium)
                    }
                }
                if (state.secureActionLoading) CircularProgressIndicator(Modifier.size(20.dp))
            }
        }
        AnimatedVisibility(state.message != null) {
            Text(
                state.message?.text.orEmpty(), textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error,
            )
        }
        IosTintedButton("Вийти", vm::signOut, Modifier.padding(top = 8.dp), tint = MaterialTheme.colorScheme.error)
    }
}
