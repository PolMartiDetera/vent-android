package com.vent.app.ui

import com.vent.app.data.model.knotsToKmh
import com.vent.app.data.model.knotsToMph
import com.vent.app.data.model.knotsToMs
import com.vent.app.data.model.round1
import com.vent.app.data.settings.TempUnit
import com.vent.app.data.settings.WaveUnit
import com.vent.app.data.settings.WindUnit
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Display-unit formatting at the edge. Internal values stay knots / meters / celsius. */
fun formatWind(kn: Double, unit: WindUnit): String = when (unit) {
    WindUnit.KNOTS -> "${kn.round1()} kn"
    WindUnit.KMH -> "${knotsToKmh(kn).round1()} km/h"
    WindUnit.MS -> "${knotsToMs(kn).round1()} m/s"
    WindUnit.MPH -> "${knotsToMph(kn).round1()} mph"
}

fun formatWave(m: Double, unit: WaveUnit): String = when (unit) {
    WaveUnit.METERS -> "${m.round1()} m"
    WaveUnit.FEET -> "${(m * 3.28084).round1()} ft"
}

fun formatTemp(c: Double, unit: TempUnit): String = when (unit) {
    TempUnit.CELSIUS -> "${c.roundToIntSafe()}°"
    TempUnit.FAHRENHEIT -> "${(c * 9 / 5 + 32).roundToIntSafe()}°"
}

private fun Double.roundToIntSafe(): Int = round1().toInt()

private val clockFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault())

fun formatClock(epochMillis: Long): String =
    Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()).format(clockFormatter)

private val dayLabelFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("EEE d MMM", Locale.getDefault())

/** "Today" / "Tomorrow" / short date for forecast-day rows. */
fun formatDayLabel(epochMillis: Long, nowEpochMillis: Long = System.currentTimeMillis()): String {
    val zone = ZoneId.systemDefault()
    val date = Instant.ofEpochMilli(epochMillis).atZone(zone).toLocalDate()
    val today = Instant.ofEpochMilli(nowEpochMillis).atZone(zone).toLocalDate()
    return when (date) {
        today -> "Today"
        today.plusDays(1) -> "Tomorrow"
        else -> date.format(dayLabelFormatter)
    }
}

/** Unicode wind-from-direction arrow (north up): NE wind shows a northeast-pointing arrow. */
fun windArrow(directionDeg: Int): String {
    val normalized = ((directionDeg % 360) + 360) % 360
    return when {
        normalized < 22 || normalized >= 338 -> "\u2191"
        normalized < 68 -> "\u2197"
        normalized < 113 -> "\u2192"
        normalized < 158 -> "\u2198"
        normalized < 203 -> "\u2193"
        normalized < 248 -> "\u2199"
        normalized < 293 -> "\u2190"
        else -> "\u2196"
    }
}