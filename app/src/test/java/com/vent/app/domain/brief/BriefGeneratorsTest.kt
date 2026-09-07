package com.vent.app.domain.brief

import com.vent.app.data.model.ForecastDay
import com.vent.app.data.model.ForecastHour
import com.vent.app.data.model.TideData
import com.vent.app.data.model.WaveData
import com.vent.app.data.model.WindData
import org.junit.Assert.assertTrue
import org.junit.Test

class BriefGeneratorsTest {

    private fun hour(wind: Double, gust: Double, dir: Int = 50, press: Double = 1013.0, k: Int) =
        ForecastHour(
            time = 1_700_000_000_000L + k * 3600_000L,
            windSpeedKnots = wind,
            windGustKnots = gust,
            windDirectionDeg = dir,
            waveHeightMeters = 0.0,
            wavePeriodSeconds = 0.0,
            tempCelsius = 20.0,
            precipMm = 0.0,
            pressureHpa = press,
            cloudCoverPct = 10,
        )

    private fun day(winds: List<Double>, gusts: List<Double>, waveMax: Double = 0.2): ForecastDay {
        val hours = winds.indices.map { hour(winds[it], gusts[it], k = it) }
        return ForecastDay(
            date = 1_700_000_000_000L,
            windMaxKnots = winds.max(),
            gustMaxKnots = gusts.max(),
            waveMaxMeters = waveMax,
            wavePeriodSeconds = 8.0,
            tempMinCelsius = 18.0,
            tempMaxCelsius = 24.0,
            precipMm = 0.0,
            windDirectionDeg = 50,
            hours = hours,
        )
    }

    @Test
    fun `day brief describes conditions in plain language`() {
        val d = day(listOf(6.0, 7.0, 5.0), listOf(10.0, 11.0, 9.0))
        val text = DayBriefGenerator().brief(d)
        assertTrue(text.contains("light breeze"))
        assertTrue(text.contains("11 kn"))
        assertTrue(text.contains("calm"))
    }

    @Test
    fun `fishing conditions on a calm rising day suggest trolling`() {
        val d = day(listOf(5.0, 6.0, 6.5), listOf(9.0, 10.0, 11.0), waveMax = 0.3)
        val wind = WindData(speedKnots = 5.0, gustKnots = 10.0, directionDeg = 50, cardinal = "NE")
        val wave = WaveData(heightMeters = 0.3, periodSeconds = 8.0, directionDeg = 120)

        val c = FishingConditionsGenerator().conditions(
            day = d,
            wind = wind,
            wave = wave,
            pressureTrend = 2.5,
            moonPhase = 0.3,
            tide = null,
        )

        assertTrue(c.method.lowercase().contains("trolling"))
        assertTrue(c.summary.lowercase().contains("light"))
        assertTrue(c.effects.lowercase().contains("rising pressure"))
    }

    @Test
    fun `rough windy day suggests shore casting and mentions shelter`() {
        val d = day(listOf(20.0, 24.0, 22.0), listOf(32.0, 36.0, 34.0), waveMax = 1.8)
        val wind = WindData(speedKnots = 20.0, gustKnots = 32.0, directionDeg = 220, cardinal = "SW")
        val wave = WaveData(heightMeters = 1.8, periodSeconds = 6.0, directionDeg = 220)

        val c = FishingConditionsGenerator().conditions(
            day = d,
            wind = wind,
            wave = wave,
            pressureTrend = -3.0,
            moonPhase = 0.5,
            tide = TideData(highTideAt = 1L, lowTideAt = 1L, heightMeters = 0.5),
        )

        assertTrue(c.method.lowercase().contains("shore casting") || c.method.lowercase().contains("bottom fishing"))
        assertTrue(c.summary.lowercase().contains("chop") || c.summary.lowercase().contains("sloppy"))
        assertTrue(c.effects.lowercase().contains("shelter"))
    }
}