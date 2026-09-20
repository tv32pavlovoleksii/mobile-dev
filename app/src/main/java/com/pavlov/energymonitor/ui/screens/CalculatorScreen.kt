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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.pavlov.energymonitor.domain.Appliance
import com.pavlov.energymonitor.domain.ApplianceInputValidator
import com.pavlov.energymonitor.domain.ConsumptionSimulator
import com.pavlov.energymonitor.domain.EnergyCalculator
import com.pavlov.energymonitor.domain.TariffPlan
import com.pavlov.energymonitor.ui.components.IosCard
import com.pavlov.energymonitor.ui.components.IosDivider
import com.pavlov.energymonitor.ui.components.IosPrimaryButton
import com.pavlov.energymonitor.ui.components.IosTextField
import com.pavlov.energymonitor.ui.components.LargeTitle
import com.pavlov.energymonitor.ui.components.MetricTile
import com.pavlov.energymonitor.ui.components.SectionHeader
import com.pavlov.energymonitor.ui.components.SegmentedControl
import com.pavlov.energymonitor.ui.components.formatDecimal
import com.pavlov.energymonitor.ui.components.iosSliderColors
import com.pavlov.energymonitor.ui.theme.energyColors

/**
 * ПЗ 2 (просунутий рівень): калькулятор енергоспоживання.
 *
 * Розподіл відповідальності (масштабованість):
 *  - domain/EnergyCalculator — усі формули (кВт·год, вартість, CO₂), без залежності від UI;
 *  - domain/ApplianceInputValidator — розбір і перевірка введення;
 *  - цей файл — лише UI-стан (список приладів, тариф) та виклики домену;
 *  - ConsumptionCharts.kt — графіки Vico (стороння залежність Gradle).
 *
 * UI-State: усі зміни (перемикач, повзунок, додавання) змінюють `appliances`/`plan`/`nightShare`,
 * а `summary` перераховується під час рекомпозиції — окремих «оновити» викликів немає.
 */
@Composable
fun CalculatorScreen(appliances: SnapshotStateList<Appliance>, modifier: Modifier = Modifier) {
    var planIndex by rememberSaveable { mutableIntStateOf(1) }
    var nightShare by rememberSaveable { mutableFloatStateOf(0.35f) }
    val plan = TariffPlan.entries[planIndex]

    val summary = EnergyCalculator.summarize(appliances, plan, nightShare.toDouble())

    Column(modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        LargeTitle("Калькулятор", "Прогноз споживання за ${EnergyCalculator.DAYS_IN_MONTH} днів")

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            MetricTile("Споживання", formatDecimal(summary.periodKwh, 1), "кВт·год", Modifier.weight(1f), MaterialTheme.colorScheme.primary)
            MetricTile("Вартість", formatDecimal(summary.cost, 0), "грн", Modifier.weight(1f), MaterialTheme.colorScheme.tertiary)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            MetricTile("На добу", formatDecimal(summary.dailyKwh, 1), "кВт·год", Modifier.weight(1f))
            MetricTile("Викиди CO₂", formatDecimal(summary.co2Kg, 0), "кг", Modifier.weight(1f), MaterialTheme.colorScheme.secondary)
        }

        SectionHeader("Тариф")
        IosCard {
            SegmentedControl(TariffPlan.entries.map { it.title }, planIndex, { planIndex = it })
            AnimatedVisibility(plan == TariffPlan.TWO_ZONE) {
                Column(Modifier.padding(top = 12.dp)) {
                    Text(
                        "Частка нічного споживання: ${(nightShare * 100).toInt()} %",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Slider(nightShare, { nightShare = it }, valueRange = 0f..1f, colors = iosSliderColors())
                    Text(
                        "День ${plan.dayRate} грн · ніч ${plan.nightRate} грн за кВт·год",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.energyColors.secondaryLabel,
                    )
                }
            }
        }

        SectionHeader("Прилади")
        IosCard(contentPadding = 0.dp, modifier = Modifier.animateContentSize()) {
            if (appliances.isEmpty()) {
                Text("Список порожній. Додайте прилад нижче.", Modifier.padding(16.dp), color = MaterialTheme.energyColors.secondaryLabel)
            }
            appliances.forEachIndexed { index, item ->
                if (index > 0) IosDivider(Modifier.padding(start = 16.dp))
                ApplianceRow(
                    item = item,
                    onChange = { appliances[index] = it },
                    onRemove = { appliances.removeAt(index) },
                )
            }
        }

        SectionHeader("Додати прилад")
        AddApplianceCard(onAdd = { name, power, hours ->
            appliances.add(Appliance(nextId(appliances), name.trim(), "🔌", power, hours))
        })

        SectionHeader("Споживання по приладах, кВт·год")
        IosCard {
            if (summary.perApplianceKwh.isEmpty()) {
                Text("Немає активних приладів", color = MaterialTheme.energyColors.secondaryLabel)
            } else {
                ApplianceBarChart(
                    labels = summary.perApplianceKwh.map { it.first.emoji },
                    values = summary.perApplianceKwh.map { it.second },
                )
            }
        }

        SectionHeader("Типовий добовий профіль, Вт")
        IosCard { DailyProfileChart(remember { ConsumptionSimulator().hourlyProfileW }) }
        Spacer(Modifier.height(8.dp))
    }
}

/** Рядок приладу: назва, потужність, перемикач і повзунок годин роботи. */
@Composable
private fun ApplianceRow(item: Appliance, onChange: (Appliance) -> Unit, onRemove: () -> Unit) {
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
            Switch(item.enabled, { onChange(item.copy(enabled = it)) })
            IconButton(onRemove) { Icon(Icons.Default.Close, "Видалити ${item.name}", tint = MaterialTheme.energyColors.secondaryLabel) }
        }
        AnimatedVisibility(item.enabled) {
            Slider(
                value = item.hoursPerDay.toFloat(),
                onValueChange = { onChange(item.copy(hoursPerDay = (it * 2).toInt() / 2.0)) },
                valueRange = 0.5f..24f,
                colors = iosSliderColors(),
            )
        }
    }
}

/** Форма додавання приладу з валідацією полів. Помилки з’являються після спроби додати. */
@Composable
private fun AddApplianceCard(onAdd: (name: String, powerW: Int, hours: Double) -> Unit) {
    var name by remember { mutableStateOf("") }
    var power by remember { mutableStateOf("") }
    var hours by remember { mutableStateOf("") }
    var submitted by remember { mutableStateOf(false) }

    val nameError = if (submitted) ApplianceInputValidator.validateName(name) else null
    val (powerValue, powerErr) = ApplianceInputValidator.parsePower(power)
    val (hoursValue, hoursErr) = ApplianceInputValidator.parseHours(hours)

    IosCard {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            IosTextField(name, { name = it }, "Назва", error = nameError)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                IosTextField(
                    power, { power = it }, "Потужність, Вт", Modifier.weight(1f),
                    error = if (submitted) powerErr else null,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                )
                IosTextField(
                    hours, { hours = it }, "Годин/добу", Modifier.weight(1f),
                    error = if (submitted) hoursErr else null,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                )
            }
            IosPrimaryButton("Додати", onClick = {
                submitted = true
                if (ApplianceInputValidator.validateName(name) == null && powerValue != null && hoursValue != null) {
                    onAdd(name, powerValue, hoursValue)
                    name = ""; power = ""; hours = ""; submitted = false
                }
            })
        }
    }
}

private fun nextId(list: List<Appliance>): Int = (list.maxOfOrNull { it.id } ?: 0) + 1

/** Початковий набір приладів; список живе в EnergyApp, щоб не губитись при зміні вкладки. */
fun defaultAppliances() = listOf(
    Appliance(1, "Холодильник", "🧊", 150, 8.0),
    Appliance(2, "Кондиціонер", "❄️", 1200, 5.0),
    Appliance(3, "Пральна машина", "🧺", 500, 1.0),
    Appliance(4, "Освітлення LED", "💡", 60, 6.0),
    Appliance(5, "Комп’ютер", "💻", 250, 7.0),
    Appliance(6, "Чайник", "☕", 2000, 0.5),
)
