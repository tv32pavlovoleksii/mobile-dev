package com.pavlov.energymonitor.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SliderColors
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pavlov.energymonitor.ui.theme.energyColors

/** Великий заголовок екрана у стилі iOS «Large Title» з необов’язковим підзаголовком. */
@Composable
fun LargeTitle(title: String, subtitle: String? = null, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth().padding(top = 8.dp, bottom = 4.dp)) {
        Text(title, style = MaterialTheme.typography.headlineLarge, color = MaterialTheme.colorScheme.onBackground)
        if (subtitle != null) {
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.energyColors.secondaryLabel)
        }
    }
}

/** Заголовок секції: дрібний, капсом-подібний, приглушеного кольору (як у grouped-списках iOS). */
@Composable
fun SectionHeader(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.energyColors.secondaryLabel,
        modifier = modifier.padding(start = 16.dp, top = 12.dp, bottom = 2.dp),
    )
}

/** Біла (у темній темі — темно-сіра) закруглена картка без тіні, як «inset grouped» у iOS. */
@Composable
fun IosCard(
    modifier: Modifier = Modifier,
    contentPadding: Dp = 16.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(Modifier.padding(contentPadding), content = content)
    }
}

/** Тонкий роздільник усередині картки. */
@Composable
fun IosDivider(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().height(0.5.dp).background(MaterialTheme.energyColors.separator))
}

/** Основна кнопка: на всю ширину, висота 50 dp, радіус 14 dp, колір акценту. */
@Composable
fun IosPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    container: Color = MaterialTheme.colorScheme.primary,
    content: (@Composable () -> Unit)? = null,
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.fillMaxWidth().height(50.dp),
        shape = MaterialTheme.shapes.medium,
        color = if (enabled) container else container.copy(alpha = 0.4f),
        contentColor = Color.White,
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            if (content != null) content()
            else Text(text, style = MaterialTheme.typography.labelLarge, color = Color.White)
        }
    }
}

/** Вторинна кнопка: «tinted» — напівпрозорий акцент на тлі й акцентний текст. */
@Composable
fun IosTintedButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, tint: Color = MaterialTheme.colorScheme.primary) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(44.dp),
        shape = MaterialTheme.shapes.medium,
        color = tint.copy(alpha = 0.14f),
    ) {
        Box(Modifier.padding(horizontal = 16.dp), contentAlignment = Alignment.Center) {
            Text(text, style = MaterialTheme.typography.labelLarge.copy(fontSize = 15.sp), color = tint)
        }
    }
}

/**
 * Сегментований контрол (UISegmentedControl): «повзунок» плавно їздить між варіантами.
 * Ширина сегмента обчислюється з доступної ширини — тому контрол адаптивний.
 */
@Composable
fun SegmentedControl(
    options: List<String>,
    selectedIndex: Int,
    onSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(
        modifier
            .fillMaxWidth()
            .height(36.dp)
            .clip(RoundedCornerShape(9.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(2.dp),
    ) {
        val segmentWidth = maxWidth / options.size
        val offset by animateDpAsState(segmentWidth * selectedIndex, spring(dampingRatio = 0.8f, stiffness = 500f), label = "segment")
        Box(
            Modifier
                .offset(x = offset)
                .width(segmentWidth)
                .height(32.dp)
                .shadow(1.dp, RoundedCornerShape(7.dp))
                .clip(RoundedCornerShape(7.dp))
                .background(MaterialTheme.colorScheme.surface),
        )
        Row(Modifier.fillMaxWidth()) {
            options.forEachIndexed { index, title ->
                Box(
                    Modifier
                        .width(segmentWidth)
                        .height(32.dp)
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onSelected(index) },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        title,
                        style = MaterialTheme.typography.labelMedium.copy(fontSize = 13.sp),
                        fontWeight = if (index == selectedIndex) FontWeight.SemiBold else FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

/** «Таблетка»-індикатор стану з кольоровою крапкою. */
@Composable
fun StatusChip(text: String, color: Color, modifier: Modifier = Modifier) {
    Surface(modifier, shape = RoundedCornerShape(50), color = color.copy(alpha = 0.15f), border = BorderStroke(0.dp, Color.Transparent)) {
        Row(Modifier.padding(horizontal = 10.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.width(8.dp).height(8.dp).clip(RoundedCornerShape(50)).background(color))
            Text(text, Modifier.padding(start = 6.dp), style = MaterialTheme.typography.labelMedium, color = color, fontWeight = FontWeight.SemiBold)
        }
    }
}

/** Плитка з одним показником: підпис, велике значення, одиниця виміру. */
@Composable
fun MetricTile(label: String, value: String, unit: String, modifier: Modifier = Modifier, accent: Color = MaterialTheme.colorScheme.onSurface) {
    IosCard(modifier, contentPadding = 14.dp) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.energyColors.secondaryLabel)
        Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.padding(top = 4.dp)) {
            Text(value, style = MaterialTheme.typography.titleLarge.copy(fontSize = 26.sp, fontWeight = FontWeight.Bold), color = accent)
            Text(unit, Modifier.padding(start = 4.dp, bottom = 3.dp), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.energyColors.secondaryLabel)
        }
    }
}

/** Слайдер у кольорах iOS: акцентна активна частина, сіра доріжка, білий бігунок. */
@Composable
fun iosSliderColors(): SliderColors = SliderDefaults.colors(
    thumbColor = Color.White,
    activeTrackColor = MaterialTheme.colorScheme.primary,
    inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant,
    activeTickColor = Color.Transparent,
    inactiveTickColor = Color.Transparent,
)
