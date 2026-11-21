package com.pavlov.energymonitor.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pavlov.energymonitor.R
import com.pavlov.energymonitor.domain.ConsumptionSimulator
import com.pavlov.energymonitor.domain.EnergyMode
import com.pavlov.energymonitor.domain.ModeForecast
import com.pavlov.energymonitor.ui.components.IosCard
import com.pavlov.energymonitor.ui.components.LargeTitle
import com.pavlov.energymonitor.ui.components.MetricTile
import com.pavlov.energymonitor.ui.components.SectionHeader
import com.pavlov.energymonitor.ui.components.SegmentedControl
import com.pavlov.energymonitor.ui.components.formatDecimal
import com.pavlov.energymonitor.ui.components.formatInt
import com.pavlov.energymonitor.ui.theme.energyColors

/**
 * ПЗ 3 (просунутий рівень): базові елементи Jetpack Compose.
 *
 * Показано: Text, Image, Button (усі типи), Row/Column/Box, Modifier-ланцюжки, State (remember),
 * декілька composable-функцій, адаптивне розташування (BoxWithConstraints).
 * Бізнес-логіка (прогноз для режиму) лежить у domain/EnergyMode.kt.
 */
@Composable
fun ComponentsScreen(modifier: Modifier = Modifier) {
    // State екрана: обраний режим і видимість порад. rememberSaveable переживає поворот екрана.
    var modeIndex by rememberSaveable { mutableIntStateOf(1) }
    var showTips by rememberSaveable { mutableStateOf(false) }
    val mode = EnergyMode.entries[modeIndex]
    val profile = remember { ConsumptionSimulator().hourlyProfileW }

    Column(modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        LargeTitle("Режими", "Прогноз залежно від сценарію")

        HeroHeader()

        SectionHeader("Режим роботи")
        ModeSelector(modeIndex, onSelect = { modeIndex = it })

        // Адаптивність: на вузькому екрані плитки одна під одною в Row по 2, на широкому — 4 в ряд.
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val wide = maxWidth > 600.dp
            val kwh = ModeForecast.dailyKwh(profile, mode)
            val peak = ModeForecast.peakW(profile, mode)
            val tiles = listOf(
                Triple("Прогноз на добу", formatDecimal(kwh, 1), "кВт·год"),
                Triple("Пік навантаження", formatInt(peak), "Вт"),
                Triple("Коефіцієнт", formatDecimal(mode.factor, 2), "×"),
                Triple("Вартість/добу", formatDecimal(kwh * 4.32, 0), "грн"),
            )
            if (wide) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    tiles.forEach { (l, v, u) -> MetricTile(l, v, u, Modifier.weight(1f)) }
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    tiles.chunked(2).forEach { pair ->
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            pair.forEach { (l, v, u) -> MetricTile(l, v, u, Modifier.weight(1f)) }
                        }
                    }
                }
            }
        }

        AdviceCard(mode = mode, expanded = showTips, onToggle = { showTips = !showTips })

        SectionHeader("Типи кнопок")
        ButtonsShowcase()
        Spacer(Modifier.height(8.dp))
    }
}

/** Шапка з зображенням (Image + painterResource) і накладеним бейджем (Box). */
@Composable
private fun HeroHeader() {
    IosCard {
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Image(
                painter = painterResource(R.drawable.ic_energy_hero),
                contentDescription = "Будинок з блискавкою",
                contentScale = ContentScale.Fit,
                modifier = Modifier.size(120.dp).clip(CircleShape),
            )
            // Box накладає елементи один на одного: бейдж у правому верхньому куті.
            Box(
                Modifier
                    .align(Alignment.TopEnd)
                    .clip(CircleShape)
                    .background(MaterialTheme.energyColors.good)
                    .padding(horizontal = 10.dp, vertical = 4.dp),
            ) { Text("Online", color = Color.White, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold) }
        }
        Text(
            "Система моніторингу енергоспоживання",
            Modifier.fillMaxWidth().padding(top = 12.dp),
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            "Оберіть режим — прогноз оновиться миттєво завдяки State.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.energyColors.secondaryLabel,
        )
    }
}

/** Вибір режиму: обробка натискання через параметр-лямбду (state hoisting). */
@Composable
private fun ModeSelector(selected: Int, onSelect: (Int) -> Unit) {
    SegmentedControl(EnergyMode.entries.map { it.title }, selected, onSelect)
}

/** Картка з порадою; текст кнопки змінюється при натисканні (базове завдання ПЗ 3). */
@Composable
private fun AdviceCard(mode: EnergyMode, expanded: Boolean, onToggle: () -> Unit) {
    IosCard {
        Text("Порада для режиму «${mode.title}»", style = MaterialTheme.typography.titleMedium)
        AnimatedVisibility(expanded) {
            AnimatedContent(mode, transitionSpec = { fadeIn() togetherWith fadeOut() }, label = "advice") {
                Text(it.advice, Modifier.padding(top = 6.dp), style = MaterialTheme.typography.bodyMedium)
            }
        }
        TextButton(onToggle, Modifier.padding(top = 4.dp)) {
            Text(if (expanded) "Сховати пораду" else "Показати пораду")
        }
    }
}

/** Усі базові типи кнопок Compose в одному місці. */
@Composable
private fun ButtonsShowcase() {
    var log by remember { mutableStateOf("Натисніть будь-яку кнопку") }
    IosCard {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Button({ log = "Button" }, Modifier.weight(1f)) { Text("Filled") }
            FilledTonalButton({ log = "FilledTonalButton" }, Modifier.weight(1f)) { Text("Tonal") }
        }
        Row(
            Modifier.padding(top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedButton({ log = "OutlinedButton" }, Modifier.weight(1f)) { Text("Outlined") }
            TextButton({ log = "TextButton" }, Modifier.weight(1f)) { Text("Text") }
            IconButton({ log = "IconButton" }) { Icon(Icons.Default.Refresh, "Оновити", tint = MaterialTheme.colorScheme.primary) }
        }
        Text("Остання дія: $log", Modifier.padding(top = 8.dp), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.energyColors.secondaryLabel)
    }
}
