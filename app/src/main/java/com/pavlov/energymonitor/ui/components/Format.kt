package com.pavlov.energymonitor.ui.components

import androidx.compose.ui.graphics.Color
import com.pavlov.energymonitor.domain.LoadLevel
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

/** Українське форматування чисел: пробіл як роздільник тисяч, кома як десятковий роздільник. */
private val symbols = DecimalFormatSymbols(Locale.forLanguageTag("uk-UA")).apply {
    groupingSeparator = ' '
    decimalSeparator = ','
}

fun formatInt(value: Int): String = DecimalFormat("#,##0", symbols).format(value)

fun formatDecimal(value: Double, digits: Int = 1): String =
    DecimalFormat(if (digits > 0) "#,##0." + "0".repeat(digits) else "#,##0", symbols).format(value)

/** Кольори статусу навантаження — беруться з теми через передані аргументи. */
fun LoadLevel.color(good: Color, warning: Color, danger: Color, normal: Color): Color = when (this) {
    LoadLevel.LOW -> good
    LoadLevel.NORMAL -> normal
    LoadLevel.HIGH -> warning
    LoadLevel.CRITICAL -> danger
}
