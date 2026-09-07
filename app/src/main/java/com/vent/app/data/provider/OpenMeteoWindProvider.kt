package com.vent.app.data.provider

import com.vent.app.data.model.DailyInfo
import com.vent.app.data.model.ForecastHour
import com.vent.app.data.model.WindRose
import com.vent.app.data.model.kmhToKnots
import com.vent.app.data.remote.OmTime
import com.vent.app.data.remote.OpenMeteoClient

/**
 * Wind + weather forecast from Open-Meteo's free forecast API (no API key).
 * Open-Meteo reports wind in km/h; we normalize to knots for the domain layer.
 */
class OpenMeteoWindProvider(
    private val client: OpenMeteoClient,
) : WindProvider {

    override suspend fun wind(lat: Double, lon: Double, time: Long): List<ForecastHour> {
        val om = client.weather(lat, lon)
        val offset = om.utcOffsetSeconds
        val hours = om.hourly
        val size = hours.time.size

        return List(size) { i ->
            ForecastHour(
                time = OmTime.toEpochMillis(hours.time.getOrElse(i) { "" }, offset),
                windSpeedKnots = kmhToKnots(hours.windSpeed10m.getOrElse(i) { 0.0 }),
                windGustKnots = kmhToKnots(hours.windGusts10m.getOrElse(i) { 0.0 }),
                windDirectionDeg = hours.windDirection10m.getOrElse(i) { 0 },
                waveHeightMeters = 0.0,
                wavePeriodSeconds = 0.0,
                tempCelsius = hours.temperature2m.getOrElse(i) { 0.0 },
                precipMm = hours.precipitation.getOrElse(i) { 0.0 },
                pressureHpa = hours.pressureMsl.getOrElse(i) { 0.0 },
                cloudCoverPct = hours.cloudCover.getOrElse(i) { 0 },
            )
        }.filter { it.time >= time - 3600_000L }
    }

    override suspend fun current(lat: Double, lon: Double, time: Long): ForecastHour {
        val om = client.weather(lat, lon)
        val c = om.current
        return ForecastHour(
            time = time,
            windSpeedKnots = kmhToKnots(c.windSpeed10m),
            windGustKnots = kmhToKnots(c.windGusts10m),
            windDirectionDeg = c.windDirection10m,
            waveHeightMeters = 0.0,
            wavePeriodSeconds = 0.0,
            tempCelsius = c.temperature2m,
            precipMm = 0.0,
            pressureHpa = c.pressureMsl,
            cloudCoverPct = c.cloudCover,
        )
    }

    override suspend fun daily(lat: Double, lon: Double): DailyInfo {
        val om = client.weather(lat, lon)
        val offset = om.utcOffsetSeconds
        val d = om.daily
        return DailyInfo(
            sunrise = firstEpoch(d.sunrise, offset),
            sunset = firstEpoch(d.sunset, offset),
            // moonrise/moonset are absent for days when the moon never rises/sets
            moonrise = firstNonEmpty(d.moonrise, offset),
            moonset = firstNonEmpty(d.moonset, offset),
            moonPhase = d.moonPhase.firstOrNull() ?: 0.0,
        )
    }

    private fun firstEpoch(list: List<String>, offset: Int): Long =
        list.firstOrNull()?.let { OmTime.toEpochMillis(it, offset) } ?: 0L

    private fun firstNonEmpty(list: List<String>, offset: Int): Long =
        list.firstOrNull { it.isNotEmpty() }?.let { OmTime.toEpochMillis(it, offset) } ?: 0L
}