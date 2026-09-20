package com.pavlov.energymonitor.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Палітра, наближена до системних кольорів iOS (Human Interface Guidelines).
 * Світла та темна версії підбираються в Theme.kt.
 */
object IosColors {
    // Акценти (Light / Dark)
    val BlueLight = Color(0xFF007AFF)
    val BlueDark = Color(0xFF0A84FF)
    val GreenLight = Color(0xFF34C759)
    val GreenDark = Color(0xFF30D158)
    val OrangeLight = Color(0xFFFF9500)
    val OrangeDark = Color(0xFFFF9F0A)
    val RedLight = Color(0xFFFF3B30)
    val RedDark = Color(0xFFFF453A)
    val YellowLight = Color(0xFFFFCC00)
    val YellowDark = Color(0xFFFFD60A)

    // Фони (systemGroupedBackground / secondarySystemGroupedBackground)
    val GroupedBackgroundLight = Color(0xFFF2F2F7)
    val GroupedBackgroundDark = Color(0xFF000000)
    val CardLight = Color(0xFFFFFFFF)
    val CardDark = Color(0xFF1C1C1E)
    val FillLight = Color(0xFFE9E9EB)   // tertiarySystemFill для сегментів і полів
    val FillDark = Color(0xFF2C2C2E)

    // Текст
    val LabelLight = Color(0xFF000000)
    val LabelDark = Color(0xFFFFFFFF)
    val SecondaryLabelLight = Color(0xFF6C6C70)
    val SecondaryLabelDark = Color(0xFF98989F)
    val SeparatorLight = Color(0x1F3C3C43)
    val SeparatorDark = Color(0x40545458)
}

/** Семантичні кольори енергетичного домену, які не входять у схему Material. */
data class EnergyColors(
    val good: Color,
    val warning: Color,
    val danger: Color,
    val secondaryLabel: Color,
    val separator: Color,
)
