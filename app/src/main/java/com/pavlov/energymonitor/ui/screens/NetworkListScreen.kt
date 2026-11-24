package com.pavlov.energymonitor.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pavlov.energymonitor.domain.NetworkNode
import com.pavlov.energymonitor.domain.NodeStatus
import com.pavlov.energymonitor.domain.NodeType
import com.pavlov.energymonitor.domain.SortOption
import com.pavlov.energymonitor.ui.components.IosCard
import com.pavlov.energymonitor.ui.components.IosTextField
import com.pavlov.energymonitor.ui.components.LargeTitle
import com.pavlov.energymonitor.ui.components.SectionHeader
import com.pavlov.energymonitor.ui.components.SegmentedControl
import com.pavlov.energymonitor.ui.components.StatusChip
import com.pavlov.energymonitor.ui.theme.energyColors
import com.pavlov.energymonitor.viewmodel.CombinedSignal
import com.pavlov.energymonitor.viewmodel.NetworkSignalViewModel
import com.pavlov.energymonitor.viewmodel.NodesViewModel
import com.pavlov.energymonitor.viewmodel.RawSignal
import com.pavlov.energymonitor.viewmodel.SmoothedSignal

/**
 * ПЗ 13 (вгорі — живий сигнал мережі) + ПЗ 14/15 (список вузлів, пошук, фільтри, сортування).
 */
@Composable
fun NetworkListScreen(
    vm: NodesViewModel,
    signalVm: NetworkSignalViewModel,
    onOpenDetail: (Int) -> Unit,
    onOpenMap: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val query by vm.query.collectAsStateWithLifecycle()
    val typeFilter by vm.typeFilter.collectAsStateWithLifecycle()
    val statusFilter by vm.statusFilter.collectAsStateWithLifecycle()
    val sortOption by vm.sortOption.collectAsStateWithLifecycle()
    val visibleNodes by vm.visibleNodes.collectAsStateWithLifecycle()

    Column(modifier) {
        LargeTitle("Мережа", "Моніторинг вузлів енергомережі в реальному часі")

        SectionHeader("Живий сигнал мережі · ПЗ 13")
        LiveSignalCard(signalVm)

        Row(Modifier.fillMaxWidth().padding(top = 12.dp)) {
            Surface(
                onClick = onOpenMap,
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    "Показати на мапі",
                    Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }

        SectionHeader("Пошук і фільтри · ПЗ 15")
        IosTextField(
            value = query,
            onValueChange = vm::onQueryChange,
            label = "Пошук за назвою",
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            modifier = Modifier.padding(top = 4.dp),
        )
        Spacer(Modifier.height(8.dp))
        TypeFilterRow(selected = typeFilter, onSelect = vm::onTypeFilterChange)
        Spacer(Modifier.height(8.dp))
        StatusFilterRow(selected = statusFilter, onSelect = vm::onStatusFilterChange)
        Spacer(Modifier.height(10.dp))
        SegmentedControl(
            options = SortOption.entries.map { it.label },
            selectedIndex = SortOption.entries.indexOf(sortOption),
            onSelected = { vm.onSortChange(SortOption.entries[it]) },
        )

        SectionHeader("Вузли мережі (${visibleNodes.size})")
        if (visibleNodes.isEmpty()) {
            IosCard { Text("Нічого не знайдено — змініть пошук або фільтри.", color = MaterialTheme.energyColors.secondaryLabel) }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                visibleNodes.forEach { node ->
                    NodeCard(node, onClick = { onOpenDetail(node.id) })
                }
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun LiveSignalCard(vm: NetworkSignalViewModel) {
    val raw by vm.rawSignal.collectAsStateWithLifecycle()
    val smoothed by vm.smoothedSignal.collectAsStateWithLifecycle()
    val combined by vm.combinedSignal.collectAsStateWithLifecycle()
    IosCard {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Датчик (базовий)", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.energyColors.secondaryLabel)
            Text(
                when (val r = raw) {
                    is RawSignal.Loading -> "…"
                    is RawSignal.Value -> "${r.watts} Вт"
                },
                style = MaterialTheme.typography.labelLarge,
            )
        }
        Row(Modifier.fillMaxWidth().padding(top = 6.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Згладжений (середній)", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.energyColors.secondaryLabel)
            Text(
                when (val s = smoothed) {
                    SmoothedSignal.Loading -> "…"
                    is SmoothedSignal.Value -> "%.1f кВт".format(s.kilowatts)
                    is SmoothedSignal.Failed -> s.message
                },
                style = MaterialTheme.typography.labelLarge,
                color = if (smoothed is SmoothedSignal.Failed) MaterialTheme.energyColors.danger else MaterialTheme.colorScheme.onSurface,
            )
        }
        Row(Modifier.fillMaxWidth().padding(top = 6.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Датчик + прогноз (просунутий)", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.energyColors.secondaryLabel)
            Text(
                when (val c = combined) {
                    CombinedSignal.Loading -> "…"
                    is CombinedSignal.Value -> "${c.sensorW} / ${c.forecastW} Вт (%+.1f%%)".format(c.deviationPct)
                    is CombinedSignal.Failed -> c.message
                },
                style = MaterialTheme.typography.labelLarge,
                color = if (combined is CombinedSignal.Failed) MaterialTheme.energyColors.danger else MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
private fun TypeFilterRow(selected: NodeType?, onSelect: (NodeType?) -> Unit) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        item { FilterPill("Усі типи", selected == null) { onSelect(null) } }
        items(NodeType.entries) { type ->
            FilterPill(type.label, selected == type) { onSelect(if (selected == type) null else type) }
        }
    }
}

@Composable
private fun StatusFilterRow(selected: NodeStatus?, onSelect: (NodeStatus?) -> Unit) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        item { FilterPill("Будь-який стан", selected == null) { onSelect(null) } }
        items(NodeStatus.entries) { status ->
            FilterPill(status.label, selected == status) { onSelect(if (selected == status) null else status) }
        }
    }
}

@Composable
private fun FilterPill(text: String, isSelected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(50),
        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Text(
            text,
            Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            style = MaterialTheme.typography.labelMedium,
            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
        )
    }
}

/** Колір індикатора стану вузла — спільний для списку, деталей і маркерів на мапі. */
@Composable
fun statusColor(status: NodeStatus): Color = when (status) {
    NodeStatus.ONLINE -> MaterialTheme.energyColors.good
    NodeStatus.WARNING -> MaterialTheme.energyColors.warning
    NodeStatus.OFFLINE -> MaterialTheme.energyColors.danger
}

@Composable
fun NodeCard(node: NetworkNode, onClick: () -> Unit, highlighted: Boolean = false, modifier: Modifier = Modifier) {
    val color = statusColor(node.status)
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        color = if (highlighted) color.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surface,
    ) {
        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.width(10.dp).height(10.dp).background(color, CircleShape))
            Column(Modifier.padding(start = 12.dp).weight(1f)) {
                Text(node.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(node.type.label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.energyColors.secondaryLabel)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("${node.powerW / 1000.0}".let { "%.1f кВт".format(node.powerW / 1000.0) }, style = MaterialTheme.typography.labelLarge)
                StatusChip(node.status.label, color, Modifier.padding(top = 4.dp))
            }
        }
    }
}
