package com.vent.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.vent.app.data.model.ForecastHour
import com.vent.app.data.model.WindRose
import com.vent.app.data.model.round1
import com.vent.app.data.settings.UserSettings
import com.vent.app.data.settings.WindUnit
import com.vent.app.domain.brief.FishingConditions
import com.vent.app.ui.formatClock
import com.vent.app.ui.formatWave
import com.vent.app.ui.formatWind
import com.vent.app.ui.windArrow

/** Wind-first hero: huge speed, direction name + arrow, gusts as quiet subtext. */
@Composable
fun HeroCard(
    speedKn: Double,
    gustKn: Double,
    directionDeg: Int,
    settings: UserSettings,
    onOpenCompass: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val cardinal = WindRose.cardinal(directionDeg)
    Card(
        onClick = onOpenCompass,
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        ),
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Text(
                text = "Now · ${cardinal}",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
            )
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = windSpokenValue(speedKn, settings.windUnit),
                    style = MaterialTheme.typography.displayLarge.copy(fontWeight = FontWeight.Bold),
                    maxLines = 1,
                    overflow = TextOverflow.Clip,
                    modifier = Modifier.semantics { heading() },
                )
            }
            Spacer(Modifier.height(4.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clearAndSetSemantics {
                    contentDescription = "from the ${WindRose.cardinalSpoken(directionDeg)}, " +
                        "gusts ${windSpokenValue(gustKn, settings.windUnit)}"
                },
            ) {
                Text(
                    text = "${windArrow(directionDeg)} $cardinal wind",
                    style = MaterialTheme.typography.titleMedium,
                )
                Spacer(Modifier.width(12.dp))
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                ) {
                    Text(
                        text = "gusts ${windSpokenValue(gustKn, settings.windUnit)}",
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Tap for compass",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f),
            )
        }
    }
}

/** One-line plain-language summary for the day. */
@Composable
fun BriefCard(brief: String, modifier: Modifier = Modifier) {
    Card(modifier = modifier.fillMaxWidth(), colors = CardDefaults.cardColors()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Day brief", style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(4.dp))
            Text(brief, style = MaterialTheme.typography.bodyLarge)
        }
    }
}

/** Plain-text fishing appraisal: describes, not scores. */
@Composable
fun FishingCard(
    conditions: FishingConditions,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
        ),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Fishing", style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(4.dp))
            Text(conditions.summary, style = MaterialTheme.typography.bodyLarge)
            Spacer(Modifier.height(8.dp))
            Text(conditions.effects, style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(12.dp))
            AssistChip(
                onClick = {},
                label = { Text("Try: ${conditions.method}") },
            )
        }
    }
}

/** Next-hour go/no-go: wind and gust trend relative to now. */
@Composable
fun NowcastCard(
    hours: List<ForecastHour>,
    settings: UserSettings,
    modifier: Modifier = Modifier,
) {
    val now = hours.firstOrNull()
    val next = hours.getOrNull(1)
    if (now == null || next == null) return
    val windDelta = next.windSpeedKnots - now.windSpeedKnots
    val gustDelta = next.windGustKnots - now.windGustKnots

    Card(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Next hour", style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                TrendCell(
                    label = "Wind",
                    value = formatWind(next.windSpeedKnots, settings.windUnit),
                    delta = windDelta,
                    deltaUnit = settings.windUnit,
                )
                TrendCell(
                    label = "Gusts",
                    value = formatWind(next.windGustKnots, settings.windUnit),
                    delta = gustDelta,
                    deltaUnit = settings.windUnit,
                )
            }
        }
    }
}

@Composable
private fun TrendCell(
    label: String,
    value: String,
    delta: Double,
    deltaUnit: WindUnit,
) {
    Column {
        Text(label, style = MaterialTheme.typography.labelMedium)
        Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        Text(
            text = if (delta > 0.5) "\u25B2 +${delta.round1()}" else if (delta < -0.5) "\u25BC ${delta.round1()}" else "\u2014 steady",
            style = MaterialTheme.typography.labelMedium,
            color = when {
                delta > 0.5 -> MaterialTheme.colorScheme.error
                delta < -0.5 -> MaterialTheme.colorScheme.primary
                else -> MaterialTheme.colorScheme.onSurfaceVariant
            },
        )
    }
}

/** Sun + moon schedule card. */
@Composable
fun EphemerisCard(
    sunrise: Long,
    sunset: Long,
    moonrise: Long,
    moonset: Long,
    moonPhase: Double,
    modifier: Modifier = Modifier,
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Sun & moon", style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                EphemerisColumn("Sunrise", formatClock(sunrise))
                EphemerisColumn("Sunset", formatClock(sunset))
                EphemerisColumn("Moonrise", if (moonrise > 0) formatClock(moonrise) else "\u2014")
                EphemerisColumn("Moonset", if (moonset > 0) formatClock(moonset) else "\u2014")
            }
            Spacer(Modifier.height(10.dp))
            Text(
                text = "Moon phase: ${moonPhaseName(moonPhase)}",
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun EphemerisColumn(label: String, value: String) {
    Column {
        Text(label, style = MaterialTheme.typography.labelMedium)
        Text(value, style = MaterialTheme.typography.titleSmall)
    }
}

private fun moonPhaseName(phase: Double): String = when {
    phase < 0.06 || phase > 0.94 -> "new moon"
    phase < 0.19 -> "waxing crescent"
    phase < 0.31 -> "first quarter"
    phase < 0.44 -> "waxing gibbous"
    phase < 0.56 -> "full moon"
    phase < 0.69 -> "waning gibbous"
    phase < 0.81 -> "last quarter"
    else -> "waning crescent"
}

/** Horizontally scrolling 24h chips: hour, wind, gust, wave. */
@Composable
fun HourlyStrip(
    hours: List<ForecastHour>,
    settings: UserSettings,
    modifier: Modifier = Modifier,
) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(hours, key = { it.time }) { hour ->
            HourChip(hour, settings)
        }
    }
}

@Composable
private fun HourChip(hour: ForecastHour, settings: UserSettings) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        tonalElevation = 1.dp,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(formatClock(hour.time), style = MaterialTheme.typography.labelMedium)
            Spacer(Modifier.height(4.dp))
            Text(
                text = formatWind(hour.windSpeedKnots, settings.windUnit),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = "${windArrow(hour.windDirectionDeg)} g${hour.windGustKnots.round1()}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = formatWave(hour.waveHeightMeters, settings.waveUnit),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun windSpokenValue(kn: Double, unit: WindUnit): String = when (unit) {
    WindUnit.KNOTS -> "${kn.round1()} knots"
    WindUnit.KMH -> "${formatWind(kn, unit)}"
    WindUnit.MS -> "${formatWind(kn, unit)}"
    WindUnit.MPH -> "${formatWind(kn, unit)}"
}