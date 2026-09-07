package com.vent.app.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class OmCurrent(
    val time: String = "",
    @SerialName("temperature_2m") val temperature2m: Double = 0.0,
    @SerialName("wind_speed_10m") val windSpeed10m: Double = 0.0,
    @SerialName("wind_gusts_10m") val windGusts10m: Double = 0.0,
    @SerialName("wind_direction_10m") val windDirection10m: Int = 0,
    @SerialName("pressure_msl") val pressureMsl: Double = 0.0,
    @SerialName("cloud_cover") val cloudCover: Int = 0,
)

@Serializable
data class OmHourly(
    val time: List<String> = emptyList(),
    @SerialName("wind_speed_10m") val windSpeed10m: List<Double> = emptyList(),
    @SerialName("wind_gusts_10m") val windGusts10m: List<Double> = emptyList(),
    @SerialName("wind_direction_10m") val windDirection10m: List<Int> = emptyList(),
    @SerialName("temperature_2m") val temperature2m: List<Double> = emptyList(),
    val precipitation: List<Double> = emptyList(),
    @SerialName("pressure_msl") val pressureMsl: List<Double> = emptyList(),
    @SerialName("cloud_cover") val cloudCover: List<Int> = emptyList(),
)

@Serializable
data class OmDaily(
    val time: List<String> = emptyList(),
    val sunrise: List<String> = emptyList(),
    val sunset: List<String> = emptyList(),
    val moonrise: List<String> = emptyList(),
    val moonset: List<String> = emptyList(),
    @SerialName("moon_phase") val moonPhase: List<Double> = emptyList(),
    @SerialName("wind_speed_10m_max") val windMax: List<Double> = emptyList(),
    @SerialName("wind_gusts_10m_max") val gustMax: List<Double> = emptyList(),
    @SerialName("temperature_2m_max") val tempMax: List<Double> = emptyList(),
    @SerialName("temperature_2m_min") val tempMin: List<Double> = emptyList(),
    @SerialName("precipitation_sum") val precipitationSum: List<Double> = emptyList(),
)

@Serializable
data class OmWeather(
    val current: OmCurrent = OmCurrent(),
    val hourly: OmHourly = OmHourly(),
    val daily: OmDaily = OmDaily(),
    @SerialName("utc_offset_seconds") val utcOffsetSeconds: Int = 0,
)

@Serializable
data class OmWaveHourly(
    val time: List<String> = emptyList(),
    @SerialName("wave_height") val waveHeight: List<Double> = emptyList(),
    @SerialName("wave_period") val wavePeriod: List<Double> = emptyList(),
    @SerialName("wave_direction") val waveDirection: List<Int> = emptyList(),
)

@Serializable
data class OmWave(
    val hourly: OmWaveHourly = OmWaveHourly(),
    @SerialName("utc_offset_seconds") val utcOffsetSeconds: Int = 0,
)