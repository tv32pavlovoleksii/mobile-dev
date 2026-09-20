package com.pavlov.energymonitor.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pavlov.energymonitor.domain.ConsumptionSimulator
import com.pavlov.energymonitor.domain.EnergyCalculator
import com.pavlov.energymonitor.domain.LoadLevel
import com.pavlov.energymonitor.domain.PowerReading
import com.pavlov.energymonitor.domain.TariffPlan
import com.pavlov.energymonitor.ui.components.IosCard
import com.pavlov.energymonitor.ui.components.IosPrimaryButton
import com.pavlov.energymonitor.ui.components.LargeTitle
import com.pavlov.energymonitor.ui.components.MetricTile
import com.pavlov.energymonitor.ui.components.SectionHeader
import com.pavlov.energymonitor.ui.components.StatusChip
import com.pavlov.energymonitor.ui.components.color
import com.pavlov.energymonitor.ui.components.formatDecimal
import com.pavlov.energymonitor.ui.components.formatInt
import com.pavlov.energymonitor.ui.theme.energyColors
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * ПЗ 1 (просунутий рівень): «Hello World» енергомоніторингу.
 *
 * Що демонструє екран:
 *  - Composable-функції та Material Theme (кнопка, текст, картки беруть кольори з теми);
 *  - State: `reading`, `history`, `updatedAt` через remember + mutableStateOf — UI сам перемальовується;
 *  - анімація зміни значення: animateIntAsState (число), animateFloatAsState (дуга), animateColorAsState (статус);
 *  - Column / Row для компонування.
 */
@Composable
fun MonitorScreen(modifier: Modifier = Modifier) {
    val simulator = remember { ConsumptionSimulator() }

    // ---- State ---------------------------------------------------------
    var reading by remember { mutableStateOf(simulator.nextReading(currentHour())) }
    var updatedAt by remember { mutableStateOf(Date()) }
    var refreshTurns by remember { mutableFloatStateOf(0f) }
    val history = remember { mutableStateListOf(reading.powerW) }

    // Дія кнопки «Оновити»: нове вимірювання -> оновлюємо стан -> UI анімується сам.
    val refresh = {
        reading = simulator.nextReading(currentHour())
        updatedAt = Date()
        refreshTurns += 1f
        history.add(reading.powerW)
        if (history.size > MAX_HISTORY) history.removeAt(0)
    }

    val cal = Calendar.getInstance()
    val todayKwh = simulator.consumedTodayKwh(cal[Calendar.HOUR_OF_DAY], cal[Calendar.MINUTE])
    val todayCost = EnergyCalculator.cost(todayKwh, TariffPlan.SINGLE)

    Column(modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        LargeTitle("Енергоспоживання", "Квартира · оновлено ${timeFormat.format(updatedAt)}")

        PowerHeroCard(reading = reading, limitW = simulator.limitW)

        val spin by animateFloatAsState(refreshTurns * 360f, tween(600, easing = FastOutSlowInEasing), label = "spin")
        IosPrimaryButton(text = "", onClick = refresh) {
            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.rotate(spin).size(20.dp))
            Text("Оновити показник", Modifier.padding(start = 8.dp), style = MaterialTheme.typography.labelLarge, color = Color.White)
        }

        SectionHeader("Електричні параметри")
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            AnimatedMetric("Напруга", reading.voltageV.toFloat(), "В", 0, Modifier.weight(1f))
            AnimatedMetric("Струм", reading.currentA.toFloat(), "А", 1, Modifier.weight(1f))
        }

        SectionHeader("Сьогодні")
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            MetricTile("Спожито", formatDecimal(todayKwh, 1), "кВт·год", Modifier.weight(1f))
            MetricTile("Орієнтовно", formatDecimal(todayCost, 0), "грн", Modifier.weight(1f))
        }

        SectionHeader("Останні вимірювання")
        IosCard { Sparkline(history.toList(), simulator.limitW) }
        Spacer(Modifier.height(8.dp))
    }
}

/** Велика картка: кільцевий індикатор, анімоване число, статус навантаження. */
@Composable
private fun PowerHeroCard(reading: PowerReading, limitW: Int) {
    val level = LoadLevel.of(reading.powerW, limitW)
    val colors = MaterialTheme.energyColors
    val target = level.color(colors.good, colors.warning, colors.danger, MaterialTheme.colorScheme.primary)

    // Три анімації від одного стану: число, дуга, колір.
    val animatedPower by animateIntAsState(reading.powerW, tween(900, easing = FastOutSlowInEasing), label = "power")
    val fraction by animateFloatAsState((reading.powerW.toFloat() / limitW).coerceIn(0f, 1f), tween(900, easing = FastOutSlowInEasing), label = "fraction")
    val color by animateColorAsState(target, tween(600), label = "levelColor")
    val track = MaterialTheme.colorScheme.surfaceVariant

    IosCard {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Поточна потужність", style = MaterialTheme.typography.labelMedium, color = colors.secondaryLabel)
            Box(Modifier.padding(vertical = 12.dp).size(220.dp), contentAlignment = Alignment.Center) {
                Canvas(Modifier.size(220.dp)) {
                    val stroke = 18.dp.toPx()
                    val inset = stroke / 2
                    val arcSize = Size(size.width - stroke, size.height - stroke)
                    drawArc(track, 135f, 270f, false, Offset(inset, inset), arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
                    drawArc(color, 135f, 270f * fraction, false, Offset(inset, inset), arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        formatInt(animatedPower),
                        style = MaterialTheme.typography.displayMedium.copy(fontSize = 52.sp),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text("Вт", style = MaterialTheme.typography.titleMedium, color = colors.secondaryLabel)
                }
            }
            StatusChip("Навантаження: ${level.title.lowercase()}", color)
            Text(
                "Ліміт підключення ${formatInt(limitW)} Вт",
                Modifier.padding(top = 8.dp),
                style = MaterialTheme.typography.bodySmall,
                color = colors.secondaryLabel,
            )
        }
    }
}

/** Метрика з плавною зміною числа: animateFloatAsState + форматування. */
@Composable
private fun AnimatedMetric(label: String, value: Float, unit: String, digits: Int, modifier: Modifier = Modifier) {
    val shown by animateFloatAsState(value, tween(700), label = label)
    MetricTile(label, formatDecimal(shown.toDouble(), digits), unit, modifier)
}

/** Міні-графік останніх вимірювань, намальований на Canvas; пунктирна лінія — ліміт. */
@Composable
private fun Sparkline(values: List<Int>, limitW: Int) {
    val bar = MaterialTheme.colorScheme.primary
    val last = MaterialTheme.colorScheme.primary
    val dim = bar.copy(alpha = 0.35f)
    Canvas(Modifier.fillMaxWidth().height(72.dp)) {
        val slots = MAX_HISTORY
        val gap = 6.dp.toPx()
        val barW = (size.width - gap * (slots - 1)) / slots
        val offset = slots - values.size
        values.forEachIndexed { i, v ->
            val h = (v.toFloat() / (limitW * 1.1f)).coerceIn(0.03f, 1f) * size.height
            drawRoundRect(
                color = if (i == values.lastIndex) last else dim,
                topLeft = Offset((offset + i) * (barW + gap), size.height - h),
                size = Size(barW, h),
                cornerRadius = CornerRadius(barW / 2.5f),
            )
        }
    }
    Text(
        "Кожен стовпчик — одне вимірювання. Натисніть «Оновити», щоб додати нове.",
        Modifier.padding(top = 8.dp),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.energyColors.secondaryLabel,
    )
}

private const val MAX_HISTORY = 12
private val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.forLanguageTag("uk-UA"))
private fun currentHour(): Int = Calendar.getInstance()[Calendar.HOUR_OF_DAY]
