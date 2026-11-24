package com.pavlov.energymonitor.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pavlov.energymonitor.data.ApplianceSort
import com.pavlov.energymonitor.domain.Appliance
import com.pavlov.energymonitor.domain.ConsumptionSimulator
import com.pavlov.energymonitor.domain.EnergyCalculator
import com.pavlov.energymonitor.domain.TariffPlan
import com.pavlov.energymonitor.ui.components.IosCard
import com.pavlov.energymonitor.ui.components.IosDivider
import com.pavlov.energymonitor.ui.components.IosPrimaryButton
import com.pavlov.energymonitor.ui.components.LargeTitle
import com.pavlov.energymonitor.ui.components.MetricTile
import com.pavlov.energymonitor.ui.components.SectionHeader
import com.pavlov.energymonitor.ui.components.SegmentedControl
import com.pavlov.energymonitor.ui.components.formatDecimal
import com.pavlov.energymonitor.ui.components.iosSliderColors
import com.pavlov.energymonitor.ui.theme.energyColors
import com.pavlov.energymonitor.viewmodel.CalculatorUiState
import com.pavlov.energymonitor.viewmodel.CalculatorViewModel

/**
 * ПЗ 2 (формули) + ПЗ 7 (MVVM) + ПЗ 8 (Room): калькулятор енергоспоживання.
 *
 * У ПЗ 2 список приладів тримав сам екран; тепер [CalculatorScreen] — лише функція
 * від [CalculatorUiState]: читає `uiState` з [CalculatorViewModel] і показує один
 * із трьох варіантів — завантаження / дані / помилка. Форма додавання приладу
 * винесена в окремий екран `ApplianceFormScreen` (ПЗ 6), до якого веде навігація (ПЗ 5).
 */
@Composable
fun CalculatorScreen(
    vm: CalculatorViewModel,
    onAddAppliance: () -> Unit,
    onEditAppliance: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by vm.uiState.collectAsStateWithLifecycle()

    when (val s = state) {
        CalculatorUiState.Loading -> CenteredState(modifier) { CircularProgressIndicator() }
        is CalculatorUiState.Error -> CenteredState(modifier) {
            Text(s.message, color = MaterialTheme.colorScheme.error)
        }
        is CalculatorUiState.Success -> CalculatorContent(s, vm, onAddAppliance, onEditAppliance, modifier)
    }
}

/** Проміжні стани екрана (завантаження/помилка) — поки Room ще не віддав перший список. */
@Composable
private fun CenteredState(modifier: Modifier, content: @Composable () -> Unit) {
    Column(modifier.fillMaxWidth().padding(top = 48.dp), horizontalAlignment = Alignment.CenterHorizontally) { content() }
}

@Composable
private fun CalculatorContent(
    s: CalculatorUiState.Success,
    vm: CalculatorViewModel,
    onAddAppliance: () -> Unit,
    onEditAppliance: (Int) -> Unit,
    modifier: Modifier,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        LargeTitle("Калькулятор", "Прогноз споживання за ${EnergyCalculator.DAYS_IN_MONTH} днів")

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            MetricTile("Споживання", formatDecimal(s.summary.periodKwh, 1), "кВт·год", Modifier.weight(1f), MaterialTheme.colorScheme.primary)
            MetricTile("Вартість", formatDecimal(s.summary.cost, 0), "грн", Modifier.weight(1f), MaterialTheme.colorScheme.tertiary)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            MetricTile("На добу", formatDecimal(s.summary.dailyKwh, 1), "кВт·год", Modifier.weight(1f))
            MetricTile("Викиди CO₂", formatDecimal(s.summary.co2Kg, 0), "кг", Modifier.weight(1f), MaterialTheme.colorScheme.secondary)
        }

        SectionHeader("Тариф")
        IosCard {
            SegmentedControl(TariffPlan.entries.map { it.title }, s.plan.ordinal, vm::setPlanIndex)
            AnimatedVisibility(s.plan == TariffPlan.TWO_ZONE) {
                Column(Modifier.padding(top = 12.dp)) {
                    Text("Частка нічного споживання: ${(s.nightShare * 100).toInt()} %", style = MaterialTheme.typography.bodyMedium)
                    Slider(s.nightShare, vm::setNightShare, valueRange = 0f..1f, colors = iosSliderColors())
                    Text(
                        "День ${s.plan.dayRate} грн · ніч ${s.plan.nightRate} грн за кВт·год",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.energyColors.secondaryLabel,
                    )
                }
            }
        }

        SectionHeader("Прилади · зберігаються в Room")
        IosCard {
            SegmentedControl(ApplianceSort.entries.map { it.title }, s.sort.ordinal, { vm.setSort(ApplianceSort.entries[it]) })
            Row(Modifier.fillMaxWidth().padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Лише ввімкнені", Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                Switch(s.onlyEnabled, vm::setOnlyEnabled)
            }
        }
        IosCard(contentPadding = 0.dp, modifier = Modifier.animateContentSize()) {
            if (s.appliances.isEmpty()) {
                Text("Список порожній. Додайте прилад нижче.", Modifier.padding(16.dp), color = MaterialTheme.energyColors.secondaryLabel)
            }
            s.appliances.forEachIndexed { index, item ->
                if (index > 0) IosDivider(Modifier.padding(start = 16.dp))
                ApplianceRow(
                    item = item,
                    onToggle = { vm.toggle(item.id, it) },
                    onHoursChange = { vm.updateHours(item.id, it) },
                    onEdit = { onEditAppliance(item.id) },
                    onRemove = { vm.remove(item.id) },
                )
            }
        }
        IosPrimaryButton("Додати прилад", onClick = onAddAppliance)

        SectionHeader("Споживання по приладах, кВт·год")
        IosCard {
            if (s.summary.perApplianceKwh.isEmpty()) {
                Text("Немає активних приладів", color = MaterialTheme.energyColors.secondaryLabel)
            } else {
                ApplianceBarChart(
                    labels = s.summary.perApplianceKwh.map { it.first.emoji },
                    values = s.summary.perApplianceKwh.map { it.second },
                )
            }
        }

        SectionHeader("Типовий добовий профіль, Вт")
        IosCard { DailyProfileChart(remember { ConsumptionSimulator().hourlyProfileW }) }
        Spacer(Modifier.height(8.dp))
    }
}

/** Рядок приладу: назва, потужність, перемикач, повзунок годин роботи та редагування/видалення. */
@Composable
private fun ApplianceRow(
    item: Appliance,
    onToggle: (Boolean) -> Unit,
    onHoursChange: (Double) -> Unit,
    onEdit: () -> Unit,
    onRemove: () -> Unit,
) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(item.emoji, style = MaterialTheme.typography.titleLarge)
            Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Text(item.name, style = MaterialTheme.typography.bodyLarge)
                Text(
                    "${item.powerW} Вт × ${formatDecimal(item.hoursPerDay, 1)} год",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.energyColors.secondaryLabel,
                )
            }
            Switch(item.enabled, onToggle)
            IconButton(onEdit) { Icon(Icons.Default.Edit, "Редагувати ${item.name}", tint = MaterialTheme.energyColors.secondaryLabel) }
            IconButton(onRemove) { Icon(Icons.Default.Close, "Видалити ${item.name}", tint = MaterialTheme.energyColors.secondaryLabel) }
        }
        AnimatedVisibility(item.enabled) {
            Slider(
                value = item.hoursPerDay.toFloat(),
                onValueChange = { onHoursChange((it * 2).toInt() / 2.0) },
                valueRange = 0.5f..24f,
                colors = iosSliderColors(),
            )
        }
    }
}
