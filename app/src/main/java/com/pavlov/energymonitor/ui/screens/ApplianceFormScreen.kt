package com.pavlov.energymonitor.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.pavlov.energymonitor.ui.components.IosCard
import com.pavlov.energymonitor.ui.components.IosPrimaryButton
import com.pavlov.energymonitor.ui.components.IosTextField
import com.pavlov.energymonitor.ui.components.LargeTitle
import com.pavlov.energymonitor.ui.components.SectionHeader
import com.pavlov.energymonitor.ui.components.SuggestionChip
import com.pavlov.energymonitor.ui.theme.energyColors
import com.pavlov.energymonitor.viewmodel.ApplianceFormViewModel

/**
 * ПЗ 6 (просунутий рівень): форма додавання/редагування приладу.
 *
 * Розбита на composable-функції ([PresetsRow], [FieldsCard], [SaveErrorBanner], [SubmitRow]),
 * кожна відповідає за свою частину форми. Валідація — `domain/ApplianceInputValidator`,
 * перевірка унікальності назви — асинхронний запит до [com.pavlov.energymonitor.data.ApplianceRepository]
 * (ПЗ 8); під час збереження кнопка показує індикатор завантаження.
 */
@Composable
fun ApplianceFormScreen(vm: ApplianceFormViewModel, onSaved: () -> Unit, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
        Column(Modifier.widthIn(max = 480.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            LargeTitle(
                if (vm.isEdit) "Редагувати прилад" else "Новий прилад",
                "Параметри використовуються в розрахунку на вкладці «Калькулятор»",
            )
            PresetsRow(vm)
            FieldsCard(vm)
            SaveErrorBanner(vm.saveError)
            SubmitRow(vm, onSaved)
            Spacer(Modifier.height(8.dp))
        }
    }
}

/** Горизонтальний ряд підказок із другого джерела даних — каталогу типових приладів. */
@Composable
private fun PresetsRow(vm: ApplianceFormViewModel) {
    Column {
        SectionHeader("Підказки з каталогу")
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            vm.presets.forEach { preset ->
                SuggestionChip(preset.emoji, preset.name, onClick = { vm.applyPreset(preset) })
            }
        }
    }
}

/** Поля форми з валідацією в реальному часі. */
@Composable
private fun FieldsCard(vm: ApplianceFormViewModel) {
    IosCard {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            IosTextField(vm.name, { vm.name = it }, "Назва приладу", error = vm.nameError)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                IosTextField(
                    vm.power, { vm.power = it }, "Потужність, Вт", Modifier.weight(1f),
                    error = vm.powerError,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                )
                IosTextField(
                    vm.hours, { vm.hours = it }, "Годин/добу", Modifier.weight(1f),
                    error = vm.hoursError,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                )
            }
        }
    }
}

/** Помилка бізнес-правила (неунікальна назва) — результат асинхронної перевірки. */
@Composable
private fun SaveErrorBanner(message: String?) {
    AnimatedVisibility(message != null) {
        Text(
            message.orEmpty(),
            Modifier.fillMaxWidth(),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error,
        )
    }
}

/** Кнопка збереження: під час запиту до бази показує індикатор і блокується. */
@Composable
private fun SubmitRow(vm: ApplianceFormViewModel, onSaved: () -> Unit) {
    IosPrimaryButton(text = "", onClick = { vm.submit(onSaved) }, enabled = !vm.saving) {
        if (vm.saving) {
            CircularProgressIndicator(Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
            Text("Зберігаємо…", Modifier.padding(start = 10.dp), style = MaterialTheme.typography.labelLarge, color = Color.White)
        } else {
            Text(if (vm.isEdit) "Зберегти зміни" else "Додати прилад", style = MaterialTheme.typography.labelLarge, color = Color.White)
        }
    }
}
