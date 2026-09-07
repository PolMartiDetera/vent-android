package com.vent.app.ui.marine

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vent.app.data.model.ForecastDay
import com.vent.app.data.model.ForecastHour
import com.vent.app.data.model.WindRose
import com.vent.app.data.model.round1
import com.vent.app.ui.formatClock
import com.vent.app.ui.formatDayLabel
import com.vent.app.ui.windArrow
import kotlin.math.roundToInt

/** 48h-to-days forecast list with expandable hourly detail. */
@Composable
fun ForecastSection(
    days: List<ForecastDay>,
    hours: List<ForecastHour>,
    expandedDate: Long?,
    onDayClick: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "Forecast",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.semantics { heading() },
        )
        WindCharts(hours)
        days.forEach { day ->
            val expanded = day.date == expandedDate
            ForecastDayRow(day, expanded = expanded, onClick = { onDayClick(day.date) })
            if (expanded) HourlyDetail(day.hours)
        }
    }
}

@Composable
private fun ForecastDayRow(day: ForecastDay, expanded: Boolean, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = if (expanded) MaterialTheme.colorScheme.surfaceContainerHigh
            else MaterialTheme.colorScheme.surfaceContainerLow,
        ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = formatDayLabel(day.date),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = "${WindRose.cardinal(day.windDirectionDeg)} ${day.windMaxKnots.round1()} kn",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            Column(horizontalAlignment = androidx.compose.ui.Alignment.End) {
                Text(
                    text = "Gusts ${day.gustMaxKnots.round1()} kn",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    text = "${day.waveMaxMeters.round1()} m · ${day.tempMinCelsius.roundToInt()}–${day.tempMaxCelsius.roundToInt()}°",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun HourlyDetail(hours: List<ForecastHour>) {
    val shown = hours.take(8)
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            shown.forEach { hour ->
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = formatClock(hour.time),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.width(0.dp))
                    Text(
                        text = "${hour.windSpeedKnots.round1()}",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = windArrow(hour.windDirectionDeg),
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
            }
        }
    }
}