package com.vent.app.data.settings

/** Display units selectable in Settings. Internal storage stays SI-plus-knots; conversion happens at the edge. */
enum class WindUnit(val label: String) {
    KNOTS("knots"),
    KMH("km/h"),
    MS("m/s"),
    MPH("mph"),
}

enum class WaveUnit(val label: String) {
    METERS("meters"),
    FEET("feet"),
}

enum class TempUnit(val label: String) {
    CELSIUS("°C"),
    FAHRENHEIT("°F"),
}

data class UserSettings(
    val windUnit: WindUnit = WindUnit.KNOTS,
    val waveUnit: WaveUnit = WaveUnit.METERS,
    val tempUnit: TempUnit = TempUnit.CELSIUS,
    val updateFreqMin: Int = 60,
    val windSource: String = "openmeteo",
    val waveSource: String = "openmeteo",
    val tideSource: String = "none",
    val dailyBrief: Boolean = true,
    val thresholdAlerts: Boolean = true,
    val darkTheme: Boolean = false,
    val useSystemTheme: Boolean = true,
    val dynamicColor: Boolean = true,
    val haptics: Boolean = true,
    val language: String = "system",
    val locationId: Long? = null,
)