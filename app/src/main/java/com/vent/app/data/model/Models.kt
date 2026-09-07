package com.vent.app.data.model

import kotlinx.serialization.Serializable

/**
 * Wind measurement. Speeds are normalized to knots internally; display units are
 * handled by the Units layer based on user Settings.
 */
@Serializable
data class WindData(
    val speedKnots: Double,
    val gustKnots: Double,
    val directionDeg: Int,
    val cardinal: String,
)

@Serializable
data class WaveData(
    val heightMeters: Double,
    val periodSeconds: Double,
    val directionDeg: Int,
)

/** Tide data is a background/secondary data point: shore-fishing notes, not a headline feature. */
@Serializable
data class TideData(
    val highTideAt: Long?,
    val lowTideAt: Long?,
    val heightMeters: Double,
)

@Serializable
data class ForecastHour(
    val time: Long,
    val windSpeedKnots: Double,
    val windGustKnots: Double,
    val windDirectionDeg: Int,
    val waveHeightMeters: Double,
    val wavePeriodSeconds: Double,
    val tempCelsius: Double,
    val precipMm: Double,
    val pressureHpa: Double,
    val cloudCoverPct: Int,
)

@Serializable
data class ForecastDay(
    val date: Long,
    val windMaxKnots: Double,
    val gustMaxKnots: Double,
    val waveMaxMeters: Double,
    val wavePeriodSeconds: Double,
    val tempMinCelsius: Double,
    val tempMaxCelsius: Double,
    val precipMm: Double,
    val windDirectionDeg: Int,
    val hours: List<ForecastHour>,
)

/** Sun/moon schedule for the current day, provided by the weather backend. */
@Serializable
data class DailyInfo(
    val sunrise: Long,
    val sunset: Long,
    val moonrise: Long,
    val moonset: Long,
    val moonPhase: Double,
)

/** Unified domain model consumed by all screens. */
@Serializable
data class WeatherPoint(
    val lat: Double,
    val lon: Double,
    val time: Long,
    val wind: WindData,
    val wave: WaveData?,
    val tempCelsius: Double,
    val pressureHpa: Double,
    val cloudCoverPct: Int,
    val sunrise: Long,
    val sunset: Long,
    val moonrise: Long,
    val moonset: Long,
    val moonPhase: Double,
    val day: ForecastDay?,
    val nextHours: List<ForecastHour>,
)
