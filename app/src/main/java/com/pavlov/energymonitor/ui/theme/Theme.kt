package com.pavlov.energymonitor.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val LightScheme: ColorScheme = lightColorScheme(
    primary = IosColors.BlueLight,
    onPrimary = Color.White,
    secondary = IosColors.GreenLight,
    tertiary = IosColors.OrangeLight,
    error = IosColors.RedLight,
    background = IosColors.GroupedBackgroundLight,
    onBackground = IosColors.LabelLight,
    surface = IosColors.CardLight,
    onSurface = IosColors.LabelLight,
    surfaceVariant = IosColors.FillLight,
    onSurfaceVariant = IosColors.SecondaryLabelLight,
    outline = IosColors.SeparatorLight,
    outlineVariant = IosColors.SeparatorLight,
    primaryContainer = IosColors.BlueLight.copy(alpha = 0.12f),
    onPrimaryContainer = IosColors.BlueLight,
)

private val DarkScheme: ColorScheme = darkColorScheme(
    primary = IosColors.BlueDark,
    onPrimary = Color.White,
    secondary = IosColors.GreenDark,
    tertiary = IosColors.OrangeDark,
    error = IosColors.RedDark,
    background = IosColors.GroupedBackgroundDark,
    onBackground = IosColors.LabelDark,
    surface = IosColors.CardDark,
    onSurface = IosColors.LabelDark,
    surfaceVariant = IosColors.FillDark,
    onSurfaceVariant = IosColors.SecondaryLabelDark,
    outline = IosColors.SeparatorDark,
    outlineVariant = IosColors.SeparatorDark,
    primaryContainer = IosColors.BlueDark.copy(alpha = 0.22f),
    onPrimaryContainer = IosColors.BlueDark,
)

private val LightEnergy = EnergyColors(
    good = IosColors.GreenLight, warning = IosColors.OrangeLight, danger = IosColors.RedLight,
    secondaryLabel = IosColors.SecondaryLabelLight, separator = IosColors.SeparatorLight,
)
private val DarkEnergy = EnergyColors(
    good = IosColors.GreenDark, warning = IosColors.OrangeDark, danger = IosColors.RedDark,
    secondaryLabel = IosColors.SecondaryLabelDark, separator = IosColors.SeparatorDark,
)

/** iOS використовує великі заокруглення: 10 dp для елементів керування, 20 dp для карток. */
private val IosShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

private val LocalEnergyColors = staticCompositionLocalOf { LightEnergy }

/** Доступ до семантичних кольорів: `MaterialTheme.energyColors.good`. */
val MaterialTheme.energyColors: EnergyColors
    @Composable @ReadOnlyComposable get() = LocalEnergyColors.current

/**
 * Тема застосунку. Динамічні кольори Material You вимкнено навмисно —
 * інтерфейс має однаковий iOS-подібний вигляд на всіх пристроях.
 */
@Composable
fun EnergyMonitorTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(LocalEnergyColors provides if (darkTheme) DarkEnergy else LightEnergy) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkScheme else LightScheme,
            typography = Typography,
            shapes = IosShapes,
            content = content,
        )
    }
}
