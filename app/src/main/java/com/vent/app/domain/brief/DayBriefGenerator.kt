package com.vent.app.domain.brief

import com.vent.app.data.model.ForecastDay
import com.vent.app.data.model.WindRose
import com.vent.app.data.model.round1
import kotlin.math.roundToInt

/**
 * Turns a day's forecast into one natural-language sentence for the Now screen
 * and the daily-brief notification.
 */
class DayBriefGenerator {

    fun brief(day: ForecastDay): String {
        val meanWind = day.hours.takeIf { it.isNotEmpty() }?.map { it.windSpeedKnots }?.average() ?: day.windMaxKnots
        val descriptor = Beaufort.descriptor(meanWind)
        val cardinal = WindRose.cardinal(day.windDirectionDeg).lowercase()
        val gustKn = day.gustMaxKnots.roundToInt()
        val wave = day.waveMaxMeters.round1()
        val sea = Beaufort.seaState(day.waveMaxMeters)

        return buildString {
            append("$descriptor $cardinal wind")
            append(", gusts to $gustKn kn")
            append(", $sea seas to $wave m")
            append(trendNote(day))
        }
    }

    private fun trendNote(day: ForecastDay): String {
        if (day.hours.size < 2) return "."
        val first = day.hours.first().windSpeedKnots
        val last = day.hours.last().windSpeedKnots
        return when {
            last < first - 3 -> ", easing by evening."
            last > first + 3 -> ", building through the day."
            else -> "."
        }
    }
}