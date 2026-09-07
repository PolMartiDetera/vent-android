package com.vent.app.ui.marine

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.compose.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianValueFormatter
import com.patrykandpatrick.vico.compose.cartesian.data.lineModel
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.compose.common.ProvideVicoTheme
import com.patrykandpatrick.vico.compose.m3.common.rememberM3VicoTheme
import com.vent.app.data.model.ForecastHour
import java.time.Instant
import java.time.ZoneId

/**
 * Vico 3 line charts over the available forecast window: wind speed + gust, and
 * wave height. Series colors follow the M3 theme (primary/secondary) via compose-m3.
 */
@Composable
fun WindCharts(hours: List<ForecastHour>, modifier: Modifier = Modifier) {
    if (hours.size < 2) return
    val x = remember(hours) { hours.map { it.time.toDouble() / 3_600_000.0 } }
    val windSeries = remember(hours) {
        listOf(
            ChartSeries("Speed", x, hours.map { it.windSpeedKnots }),
            ChartSeries("Gust", x, hours.map { it.windGustKnots }),
        )
    }
    val waveSeries = remember(hours) {
        listOf(
            ChartSeries("Height", x, hours.map { it.waveHeightMeters }),
        )
    }
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        LineChartCard(
            title = "Wind · next ${hours.size} h",
            series = windSeries,
        )
        LineChartCard(
            title = "Waves · next ${hours.size} h",
            series = waveSeries,
        )
    }
}

private data class ChartSeries(val name: String, val x: List<Double>, val y: List<Double>)

@Composable
private fun LineChartCard(title: String, series: List<ChartSeries>) {
    val colors = listOf(
        MaterialTheme.colorScheme.primary,
        MaterialTheme.colorScheme.secondary,
        MaterialTheme.colorScheme.tertiary,
    ).take(series.size)
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            if (series.size > 1) {
                LegendRow(series.map { it.name }, colors)
            }
            LineChart(series = series, colors = colors, modifier = Modifier.fillMaxWidth().height(180.dp))
        }
    }
}

@Composable
private fun LegendRow(labels: List<String>, colors: List<Color>) {
    Row(
        modifier = Modifier.padding(top = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        labels.forEachIndexed { index, label ->
            Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                Box(
                    Modifier.size(9.dp).background(colors[index], CircleShape),
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun LineChart(series: List<ChartSeries>, colors: List<Color>, modifier: Modifier) {
    val theme = rememberM3VicoTheme(lineCartesianLayerColors = colors)
    val modelProducer = remember { CartesianChartModelProducer() }
    val valueFormatter = remember {
        CartesianValueFormatter { _, value, _ -> value.toInt().toString() }
    }
    val hourFormatter = remember {
        val zone = ZoneId.systemDefault()
        CartesianValueFormatter { _, value, _ ->
            val hour = Instant.ofEpochSecond((value * 3600).toLong()).atZone(zone).hour
            "%02d:00".format(hour)
        }
    }

    LaunchedEffect(series) {
        modelProducer.runTransaction {
            lineModel {
                series.forEach { s ->
                    series(x = s.x, y = s.y, key = s.name)
                }
            }
        }
    }

    ProvideVicoTheme(theme) {
        CartesianChartHost(
            chart = rememberCartesianChart(
                rememberLineCartesianLayer(),
                startAxis = VerticalAxis.rememberStart(valueFormatter = valueFormatter),
                bottomAxis = HorizontalAxis.rememberBottom(valueFormatter = hourFormatter),
            ),
            modelProducer = modelProducer,
            modifier = modifier,
        )
    }
}