package com.vent.app.data

import com.vent.app.data.settings.WindUnit
import com.vent.app.data.settings.WaveUnit
import com.vent.app.data.settings.TempUnit
import kotlin.math.roundToInt

/** Formatting for display units. Internal storage stays SI-plus-knots; conversion happens at the edge. */
object Units {

    /** Format wind speed in the selected unit. */
    fun format(kn: Double, unit: WindUnit): String = when (unit) {
        WindUnit.KNOTS -> "${roundToString(kn)} kn"
        WindUnit.KMH -> "${knotsToKmh(kn).roundToString1()} km/h"
        WindUnit.MS -> "${knotsToMs(kn).roundToString1()} m/s"
        WindUnit.MPH -> "${knotsToMph(kn).roundToString1()} mph"
    }

    /** Format wave height in the selected unit. */
    fun format(m: Double, unit: WaveUnit): String = when (unit) {
        WaveUnit.METERS -> "${roundToString(m)} m"
        WaveUnit.FEET -> "${((m * 3.28084).roundToString1())} ft"
    }

    /** Format temperature in the selected unit. */
    fun format(c: Double, unit: TempUnit): String = when (unit) {
        TempUnit.CELSIUS -> "${roundToString(c)}°"
        TempUnit.FAHRENHEIT -> "${((c * 9 / 5 + 32).roundToString1())}°"
    }

    /** Format distance in the selected unit. */
    fun formatDist(d: Double, unit: WindUnit): String = when (unit) {
        WindUnit.KNOTS -> "${roundToString(d)} nmi"
        WindUnit.KMH -> "${knotsToKmh(d).roundToString1()} km"
        WindUnit.MS -> "${knotsToMs(d).roundToString1()} m"
        WindUnit.MPH -> "${knotsToMph(d).roundToString1()} mi"
    }

    /** Internal conversion: knots -> km/h */
    private fun knotsToKmh(kn: Double): Double = kn * 1.852

    /** Internal conversion: knots -> m/s */
    private fun knotsToMs(kn: Double): Double = kn * 0.514444

    /** Internal conversion: knots -> mph */
    private fun knotsToMph(kn: Double): Double = kn * 0.621371

    /** Round to one decimal for display, avoiding floating-point noise. */
    private fun round1(d: Double): Double = (d * 10.0).roundToInt() / 10.0

    /** Format a rounded double, stripping ".0" when the value is a whole number. */
    private fun roundToString(d: Double): String {
        val r = round1(d)
        return if (r == r.toInt().toDouble()) r.toInt().toString() else r.toString()
    }

    /** Round to one decimal and format as string, stripping ".0" when whole. */
    private fun Double.roundToString1(): String {
        val r = round1(this)
        return if (r == r.toInt().toDouble()) r.toInt().toString() else r.toString()
    }
}