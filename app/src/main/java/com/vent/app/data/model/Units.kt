package com.vent.app.data.model

import kotlin.math.roundToInt

/** Conversion helpers. Internal canonical units: knots for wind, meters for waves. */
const val KMH_PER_KNOT = 1.852

fun kmhToKnots(kmh: Double): Double = kmh / KMH_PER_KNOT

fun metersToKnots(mps: Double): Double = mps * 1.9438444924406

fun knotsToKmh(kn: Double): Double = kn * KMH_PER_KNOT

fun knotsToMs(kn: Double): Double = kn * 0.5144444444444445

fun knotsToMph(kn: Double): Double = kn * 1.1507794480235425

fun normalizeDegrees(deg: Int): Int = ((deg % 360) + 360) % 360

object WindRose {
    private val points =
        listOf("N", "NNE", "NE", "ENE", "E", "ESE", "SE", "SSE", "S", "SSW", "SW", "WSW", "W", "WNW", "NW", "NNW")

    /** 16-point cardinal name for a meteorological wind direction (where the wind comes FROM). */
    fun cardinal(deg: Int): String {
        val d = normalizeDegrees(deg)
        val index = ((d + 11.25) / 22.5).toInt() % 16
        return points[index]
    }

    /** Keep names friendly for speech synthesis (TalkBack reads "north-northeast", not "NNE"). */
    fun cardinalSpoken(deg: Int): String = when (cardinal(deg)) {
        "N" -> "north"
        "NNE" -> "north-northeast"
        "NE" -> "northeast"
        "ENE" -> "east-northeast"
        "E" -> "east"
        "ESE" -> "east-southeast"
        "SE" -> "southeast"
        "SSE" -> "south-southeast"
        "S" -> "south"
        "SSW" -> "south-southwest"
        "SW" -> "southwest"
        "WSW" -> "west-southwest"
        "W" -> "west"
        "WNW" -> "west-northwest"
        "NW" -> "northwest"
        "NNW" -> "north-northwest"
        else -> "north"
    }.let { it + ", ${normalizeDegrees(deg)} degrees" }

    fun directionToDegrees(cardinal16: String): Int {
        val index = points.indexOf(cardinal16)
        return if (index < 0) 0 else index * 22
    }
}

/** Round to one decimal for display, avoiding floating-point noise. */
fun Double.round1(): Double = (this * 10).roundToInt() / 10.0

fun Int.roundTo16Points(): Int = ((normalizeDegrees(this) + 11) / 22.5).toInt() % 16 * 22