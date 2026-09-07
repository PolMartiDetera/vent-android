package com.vent.app.domain.alerts

import com.vent.app.data.model.ForecastDay
import com.vent.app.data.model.ForecastHour
import com.vent.app.data.model.WaveData
import com.vent.app.data.model.WeatherPoint
import com.vent.app.data.model.WindData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ThresholdAlertEngineTest {

    private val base = 1_700_000_000_000L

    private fun hour(wind: Double, gust: Double, wave: Double, k: Int) = ForecastHour(
        time = base + k * 3600_000L,
        windSpeedKnots = wind,
        windGustKnots = gust,
        windDirectionDeg = 40,
        waveHeightMeters = wave,
        wavePeriodSeconds = 7.0,
        tempCelsius = 20.0,
        precipMm = 0.0,
        pressureHpa = 1013.0,
        cloudCoverPct = 10,
    )

    private fun point(winds: List<Double>, gusts: List<Double>, waves: List<Double>): WeatherPoint {
        val hours = winds.indices.map { hour(winds[it], gusts[it], waves[it], it) }
        return WeatherPoint(
            lat = 43.0,
            lon = 4.0,
            time = base,
            wind = WindData(winds.first(), gusts.first(), 40, "NE"),
            wave = WaveData(waves.first(), 7.0, 40),
            tempCelsius = 20.0,
            pressureHpa = 1013.0,
            cloudCoverPct = 10,
            sunrise = 0,
            sunset = 0,
            moonrise = 0,
            moonset = 0,
            moonPhase = 0.0,
            day = ForecastDay(
                date = base,
                windMaxKnots = winds.max(),
                gustMaxKnots = gusts.max(),
                waveMaxMeters = waves.max(),
                wavePeriodSeconds = 7.0,
                tempMinCelsius = 18.0,
                tempMaxCelsius = 24.0,
                precipMm = 0.0,
                windDirectionDeg = 40,
                hours = hours,
            ),
            nextHours = hours,
        )
    }

    @Test
    fun `rule that crosses threshold returns a triggered result`() {
        val p = point(
            winds = listOf(8.0, 10.0, 18.0, 22.0, 14.0),
            gusts = listOf(12.0, 15.0, 26.0, 30.0, 18.0),
            waves = listOf(0.4, 0.6, 1.2, 1.6, 0.8),
        )
        val rule = AlertRule(id = 1, metric = AlertMetric.WIND_SPEED, threshold = 20.0)

        val results = ThresholdAlertEngine().evaluate(listOf(rule), p)

        assertEquals(1, results.size)
        assertEquals(22.0, results[0].peakValue, 0.0)
        assertEquals(base + 3 * 3600_000L, results[0].triggeredAt)
        assertTrue(results[0].message.contains("above 20.0"))
    }

    @Test
    fun `rule that never crosses returns empty`() {
        val p = point(
            winds = listOf(4.0, 5.0, 6.0),
            gusts = listOf(7.0, 8.0, 9.0),
            waves = listOf(0.2, 0.3, 0.4),
        )
        val rule = AlertRule(id = 2, metric = AlertMetric.WIND_SPEED, threshold = 25.0)

        val results = ThresholdAlertEngine().evaluate(listOf(rule), p)

        assertTrue(results.isEmpty())
    }

    @Test
    fun `gust and wave rules each track their own metric`() {
        val p = point(
            winds = listOf(8.0, 10.0, 12.0),
            gusts = listOf(14.0, 18.0, 22.0),
            waves = listOf(0.5, 1.0, 2.2),
        )
        val rules = listOf(
            AlertRule(id = 3, metric = AlertMetric.WIND_GUST, threshold = 20.0),
            AlertRule(id = 4, metric = AlertMetric.WAVE_HEIGHT, threshold = 2.0),
        )

        val results = ThresholdAlertEngine().evaluate(rules, p)

        assertEquals(2, results.size)
        val gust = results.first { it.rule.id == 3L }
        val wave = results.first { it.rule.id == 4L }
        assertEquals(22.0, gust.peakValue, 0.0)
        assertEquals(2.2, wave.peakValue, 0.0)
        // 2.2 >= 2.0*1.4 and >= 3.0? no -> 2.2 < 3.0, and 2.2 >= 2.8? no -> THRESHOLD. gust 22 >= 28? no -> THRESHOLD
        assertEquals(AlertSeverity.THRESHOLD, gust.severity)
        assertEquals(AlertSeverity.THRESHOLD, wave.severity)
    }

    @Test
    fun `large overshoot escalates to severe`() {
        val p = point(
            winds = listOf(10.0, 12.0, 35.0),
            gusts = listOf(15.0, 18.0, 45.0),
            waves = listOf(0.5, 0.8, 1.0),
        )
        val rule = AlertRule(id = 5, metric = AlertMetric.WIND_SPEED, threshold = 20.0)

        val results = ThresholdAlertEngine().evaluate(listOf(rule), p)

        assertEquals(1, results.size)
        assertEquals(AlertSeverity.SEVERE, results[0].severity)
    }
}