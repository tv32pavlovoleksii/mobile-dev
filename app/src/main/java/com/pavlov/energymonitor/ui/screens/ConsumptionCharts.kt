package com.pavlov.energymonitor.ui.screens

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberBottom
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberStart
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberColumnCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.compose.cartesian.rememberVicoScrollState
import com.patrykandpatrick.vico.compose.cartesian.rememberVicoZoomState
import com.patrykandpatrick.vico.core.cartesian.Zoom
import com.patrykandpatrick.vico.core.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.core.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.core.cartesian.data.CartesianValueFormatter
import com.patrykandpatrick.vico.core.cartesian.data.columnSeries
import com.patrykandpatrick.vico.core.cartesian.data.lineSeries

/**
 * Графіки на бібліотеці Vico (стороння залежність із Gradle, ПЗ 2).
 * Дані передаються як прості списки — компонент нічого не знає про доменні класи.
 */

/** Стовпчики: споживання кожного приладу за місяць, кВт·год. */
@Composable
fun ApplianceBarChart(labels: List<String>, values: List<Double>, modifier: Modifier = Modifier) {
    val producer = remember { CartesianChartModelProducer() }
    // Зміна списку => нова транзакція => Vico анімує перехід між наборами даних.
    LaunchedEffect(values) {
        if (values.isNotEmpty()) producer.runTransaction { columnSeries { series(values) } }
    }
    val formatter = remember(labels) {
        CartesianValueFormatter { _, x, _ -> labels.getOrNull(x.toInt()).orEmpty() }
    }
    CartesianChartHost(
        chart = rememberCartesianChart(
            rememberColumnCartesianLayer(),
            startAxis = VerticalAxis.rememberStart(),
            bottomAxis = HorizontalAxis.rememberBottom(
                valueFormatter = formatter,
                itemPlacer = HorizontalAxis.ItemPlacer.aligned(spacing = { 1 }),
            ),
        ),
        modelProducer = producer,
        scrollState = rememberVicoScrollState(scrollEnabled = false),
        zoomState = rememberVicoZoomState(zoomEnabled = false, initialZoom = remember { Zoom.Content }),
        modifier = modifier.fillMaxWidth().height(200.dp),
    )
}

/** Лінія: типовий добовий профіль навантаження, Вт по годинах. */
@Composable
fun DailyProfileChart(profileW: List<Int>, modifier: Modifier = Modifier) {
    val producer = remember { CartesianChartModelProducer() }
    LaunchedEffect(profileW) {
        producer.runTransaction { lineSeries { series(profileW) } }
    }
    val hourFormatter = remember { CartesianValueFormatter { _, x, _ -> "%02d".format(x.toInt()) } }
    CartesianChartHost(
        chart = rememberCartesianChart(
            rememberLineCartesianLayer(),
            startAxis = VerticalAxis.rememberStart(),
            bottomAxis = HorizontalAxis.rememberBottom(
                valueFormatter = hourFormatter,
                itemPlacer = HorizontalAxis.ItemPlacer.aligned(spacing = { 3 }),
            ),
        ),
        modelProducer = producer,
        scrollState = rememberVicoScrollState(scrollEnabled = false),
        zoomState = rememberVicoZoomState(zoomEnabled = false, initialZoom = remember { Zoom.Content }),
        modifier = modifier.fillMaxWidth().height(180.dp),
    )
}
