package com.vent.app.data.provider

import com.vent.app.data.model.ForecastHour
import com.vent.app.data.remote.OmTime
import com.vent.app.data.remote.OpenMeteoClient

/**
 * Wave forecast from Open-Meteo's marine API (same free endpoint family).
 * Fields unrelated to waves are zero-filled; the wave fields carry the data.
 */
class OpenMeteoWaveProvider(
    private val client: OpenMeteoClient,
) : WaveProvider {

    override suspend fun wave(lat: Double, lon: Double, time: Long): List<ForecastHour> {
        val om = client.wave(lat, lon)
        val offset = om.utcOffsetSeconds
        val hours = om.hourly
        val size = hours.time.size

        return List(size) { i ->
            ForecastHour(
                time = OmTime.toEpochMillis(hours.time.getOrElse(i) { "" }, offset),
                windSpeedKnots = 0.0,
                windGustKnots = 0.0,
                windDirectionDeg = hours.waveDirection.getOrElse(i) { 0 },
                waveHeightMeters = hours.waveHeight.getOrElse(i) { 0.0 },
                wavePeriodSeconds = hours.wavePeriod.getOrElse(i) { 0.0 },
                tempCelsius = 0.0,
                precipMm = 0.0,
                pressureHpa = 0.0,
                cloudCoverPct = 0,
            )
        }.filter { it.time >= time - 3600_000L }
    }
}