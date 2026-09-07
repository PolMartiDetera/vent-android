package com.vent.app.domain.brief

import com.vent.app.data.model.ForecastDay
import com.vent.app.data.model.TideData
import com.vent.app.data.model.WaveData
import com.vent.app.data.model.WindData
import com.vent.app.data.model.WindRose

/**
 * Plain-language fishing appraisal. Deliberately NOT a score: tells the angler
 * what conditions are like, how they affect fishing, and a suggested technique.
 */
data class FishingConditions(
    val summary: String,
    val effects: String,
    val method: String,
)

class FishingConditionsGenerator {

    /**
     * @param pressureTrend change in pressure (hPa) across the forecast window:
     *   positive = rising, negative = falling.
     * @param tide        optional tide window (background/secondary signal).
     */
    fun conditions(
        day: ForecastDay,
        wind: WindData,
        wave: WaveData?,
        pressureTrend: Double,
        moonPhase: Double,
        tide: TideData?,
    ): FishingConditions {
        val meanWind = day.hours.takeIf { it.isNotEmpty() }?.map { it.windSpeedKnots }?.average() ?: wind.speedKnots
        val waveMax = wave?.heightMeters ?: day.waveMaxMeters
        val rough = meanWind >= 17.0 || waveMax >= 1.0
        val calm = meanWind <= 10.0 && waveMax <= 0.5
        val rising = pressureTrend >= 1.0
        val falling = pressureTrend <= -1.0

        return FishingConditions(
            summary = summary(day, meanWind, waveMax, pressureTrend),
            effects = effects(meanWind, waveMax, day, rising, falling, moonPhase, tide),
            method = method(rough, calm, rising),
        )
    }

    private fun summary(day: ForecastDay, meanWind: Double, waveMax: Double, trend: Double): String {
        val descriptor = Beaufort.descriptor(meanWind)
        val cardinal = WindRose.cardinal(day.windDirectionDeg).lowercase()
        val direction = if (day.hours.isNotEmpty()) {
            day.hours.maxByOrNull { it.windSpeedKnots }?.windDirectionDeg
                ?.let { WindRose.cardinal(it).lowercase() } ?: cardinal
        } else cardinal
        val windNote = when {
            meanWind >= 17.0 -> "Firm ${direction} wind with a real chop"
            meanWind >= 11.0 -> "Working ${direction} breeze"
            else -> "Light ${direction} wind"
        }
        val seaNote = when {
            waveMax >= 1.5 -> ", sloppy seas."
            waveMax >= 1.0 -> ", lumpy swell."
            else -> ", tidy water."
        }
        val pressure = when {
            trend >= 1.0 -> " Pressure on the rise."
            trend <= -1.0 -> " Pressure falling."
            else -> " Pressure steady."
        }
        return "$windNote$seaNote$pressure"
    }

    private fun effects(
        meanWind: Double,
        waveMax: Double,
        day: ForecastDay,
        rising: Boolean,
        falling: Boolean,
        moonPhase: Double,
        tide: TideData?,
    ): String {
        val windBit = when {
            meanWind >= 17.0 -> "Wind pins bait down and makes light tackle hard to control"
            meanWind >= 11.0 -> "A decent breeze helps work drifting baits"
            else -> "Flat water keeps bait visible"
        }
        val waveBit = when {
            waveMax >= 1.5 -> "; fish shelter in the lee of reefs and headlands."
            waveMax >= 1.0 -> "; look for fish behind structure."
            else -> "; fish can be widespread and shallow."
        }
        val lightBit = when {
            moonPhase <= 0.1 || moonPhase >= 0.9 -> "A dark moon favours early-morning bites."
            moonPhase in 0.45..0.55 -> "The full moon keeps fish active after dark."
            else -> "Normal feeding windows around dawn and dusk."
        }
        val tideBit = when {
            tide?.lowTideAt != null -> " Plan around the tide change."
            else -> ""
        }
        val pressureBit = when {
            rising -> " Rising pressure usually sharpens bites."
            falling -> " Falling pressure can switch fish off ahead of a front."
            else -> ""
        }
        return "$windBit$waveBit $lightBit$tideBit$pressureBit"
    }

    private fun method(rough: Boolean, calm: Boolean, rising: Boolean): String {
        return when {
            rough -> "Shore casting or bottom fishing from sheltered water"
            calm && rising -> "Trolling or drifting over open water"
            calm -> "Casting into structure and drop-offs"
            else -> "Drift fishing with weighted baits"
        }
    }
}