package com.vent.app.data.remote

import com.vent.app.data.remote.dto.OmWave
import com.vent.app.data.remote.dto.OmWeather
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter

/**
 * Thin typed client over the Open-Meteo forecast + marine APIs.
 * No caching here — consumers (providers/repositories) own freshness policy.
 */
class OpenMeteoClient(
    private val http: HttpClient,
    private val baseUrl: String = "https://api.open-meteo.com/v1/forecast",
) {
    suspend fun weather(lat: Double, lon: Double): OmWeather = http.get(baseUrl) {
        parameter("latitude", lat)
        parameter("longitude", lon)
        parameter(
            "current",
            "temperature_2m,wind_speed_10m,wind_gusts_10m,wind_direction_10m,pressure_msl,cloud_cover",
        )
        parameter(
            "hourly",
            "temperature_2m,wind_speed_10m,wind_gusts_10m,wind_direction_10m,precipitation,pressure_msl,cloud_cover",
        )
        parameter(
            "daily",
            "sunrise,sunset,moonrise,moonset,moon_phase,wind_speed_10m_max,wind_gusts_10m_max," +
                "temperature_2m_max,temperature_2m_min,precipitation_sum",
        )
        parameter("timezone", "auto")
    }.body()

    suspend fun wave(lat: Double, lon: Double): OmWave = http.get(baseUrl) {
        parameter("latitude", lat)
        parameter("longitude", lon)
        parameter("hourly", "wave_height,wave_period,wave_direction")
        parameter("timezone", "auto")
    }.body()
}