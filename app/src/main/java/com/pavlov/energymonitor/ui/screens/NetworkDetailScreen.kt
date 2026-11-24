package com.pavlov.energymonitor.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pavlov.energymonitor.domain.NetworkNode
import com.pavlov.energymonitor.ui.components.IosCard
import com.pavlov.energymonitor.ui.components.IosDivider
import com.pavlov.energymonitor.ui.components.IosPrimaryButton
import com.pavlov.energymonitor.ui.components.LargeTitle
import com.pavlov.energymonitor.ui.components.SectionHeader
import com.pavlov.energymonitor.ui.components.StatusChip
import com.pavlov.energymonitor.ui.theme.energyColors
import java.text.SimpleDateFormat
import java.util.Locale

private val detailDateFormat = SimpleDateFormat("dd.MM HH:mm:ss", Locale.forLanguageTag("uk-UA"))

/**
 * ПЗ 14 (середній рівень): детальний екран вузла, відкривається кліком на картку у списку.
 * Кнопка внизу веде на мапу (ПЗ 16) з уже обраним (через спільну [NodesViewModel]) вузлом.
 */
@Composable
fun NetworkDetailScreen(node: NetworkNode?, onShowOnMap: () -> Unit, modifier: Modifier = Modifier) {
    if (node == null) {
        Column(modifier) { Text("Вузол не знайдено.") }
        return
    }
    Column(modifier) {
        LargeTitle(node.name, node.type.label)

        IosCard {
            Row(Modifier.fillMaxWidth()) { StatusChip(node.status.label, statusColor(node.status)) }
            IosDivider(Modifier.padding(vertical = 12.dp))
            DetailRow("Потужність", "%.2f кВт".format(node.powerW / 1000.0))
            DetailRow("Координати", "%.4f, %.4f".format(node.lat, node.lon))
            DetailRow("Оновлено", detailDateFormat.format(node.lastUpdated))
        }

        SectionHeader("Опис")
        IosCard { Text(node.description, style = MaterialTheme.typography.bodyMedium) }

        Spacer(Modifier.height(16.dp))
        IosPrimaryButton("Показати на мапі", onClick = onShowOnMap)
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.energyColors.secondaryLabel, modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}
