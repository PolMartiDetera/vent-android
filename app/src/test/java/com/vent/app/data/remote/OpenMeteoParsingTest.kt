package com.vent.app.data.remote

import com.vent.app.data.model.WindRose
import com.vent.app.data.model.kmhToKnots
import com.vent.app.data.remote.dto.OmWeather
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OpenMeteoParsingTest {
    private val json = Json { ignoreUnknownKeys = true }

    private val fixture = """
        {
          "latitude": 43.4,
          "longitude": 4.7,
          "generationtime_ms": 0.9,
          "utc_offset_seconds": 7200,
          "timezone": "Europe/Paris",
          "timezone_abbreviation": "CEST",
          "elevation": 2.0,
          "current_units": {
            "time": "iso8601",
            "interval": "seconds",
            "temperature_2m": "°C",
            "wind_speed_10m": "km/h",
            "wind_gusts_10m": "km/h",
            "wind_direction_10m": "°"
          },
          "current": {
            "time": "2026-09-07T10:00",
            "interval": 900,
            "temperature_2m": 21.5,
            "wind_speed_10m": 18.5,
            "wind_gusts_10m": 29.0,
            "wind_direction_10m": 22,
            "pressure_msl": 1013.2,
            "cloud_cover": 40
          },
          "hourly_units": {
            "time": "iso8601",
            "temperature_2m": "°C",
            "wind_speed_10m": "km/h",
            "wind_gusts_10m": "km/h",
            "wind_direction_10m": "°"
          },
          "hourly": {
            "time": ["2026-09-07T10:00", "2026-09-07T11:00", "2026-09-07T12:00"],
            "temperature_2m": [21.5, 22.0, 22.4],
            "wind_speed_10m": [18.5, 19.2, 20.1],
            "wind_gusts_10m": [29.0, 30.1, 31.2],
            "wind_direction_10m": [22, 30, 40],
            "precipitation": [0.0, 0.0, 0.2],
            "pressure_msl": [1013.2, 1013.0, 1012.8],
            "cloud_cover": [40, 55, 70]
          },
          "daily_units": {
            "time": "iso8601",
            "sunrise": "iso8601",
            "sunset": "iso8601"
          },
          "daily": {
            "time": ["2026-09-07"],
            "sunrise": ["2026-09-07T06:15"],
            "sunset": ["2026-09-07T19:55"],
            "moonrise": ["2026-09-07T03:20"],
            "moonset": ["2026-09-07T18:40"],
            "moon_phase": [0.42],
            "wind_speed_10m_max": [22.4],
            "wind_gusts_10m_max": [33.0],
            "temperature_2m_max": [24.1],
            "temperature_2m_min": [16.2],
            "precipitation_sum": [0.2]
          }
        }
    """.trimIndent()

    @Test
    fun `decodes weather fixture and reads current values`() {
        val parsed = json.decodeFromString<OmWeather>(fixture)
        assertEquals(21.5, parsed.current.temperature2m, 0.0)
        assertEquals(18.5, parsed.current.windSpeed10m, 0.0)
        assertEquals(22, parsed.current.windDirection10m)
        assertEquals(3, parsed.hourly.time.size)
        assertEquals(7200, parsed.utcOffsetSeconds)
        assertEquals(0.42, parsed.daily.moonPhase.first(), 0.0)
    }

    @Test
    fun `hourly wind speeds convert from kmh to knots`() {
        val parsed = json.decodeFromString<OmWeather>(fixture)
        val knots = parsed.hourly.windSpeed10m.map(::kmhToKnots)
        // 18.5 km/h ≈ 9.99 kn
        assertEquals(9.99, knots.first(), 0.01)
    }

    @Test
    fun `local times parse to true epoch using utc offset`() {
        val parsed = json.decodeFromString<OmWeather>(fixture)
        // "2026-09-07T10:00" parsed as UTC (= 10:00Z), shifted +2h = 12:00Z
        val epoch = com.vent.app.data.remote.OmTime.toEpochMillis("2026-09-07T10:00", parsed.utcOffsetSeconds)
        assertEquals(1_788_782_400_000L, epoch)
    }

    @Test
    fun `wind rose maps degrees to 16-point cardinals`() {
        assertEquals("N", WindRose.cardinal(0))
        assertEquals("N", WindRose.cardinal(360))
        assertEquals("NNE", WindRose.cardinal(22))
        assertEquals("E", WindRose.cardinal(90))
        assertEquals("S", WindRose.cardinal(180))
        assertEquals("W", WindRose.cardinal(270))
        assertEquals("NW", WindRose.cardinal(315))
        assertTrue(WindRose.cardinal(350).startsWith("N"))
    }

    @Test
    fun `decoding tolerates missing optional fields`() {
        val minimal = """{"current": {"time": "2026-09-07T10:00", "wind_speed_10m": 10.0}}"""
        val parsed = json.decodeFromString<OmWeather>(minimal)
        assertEquals(10.0, parsed.current.windSpeed10m, 0.0)
        assertTrue(parsed.hourly.time.isEmpty())
    }
}