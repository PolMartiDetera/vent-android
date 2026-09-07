package com.vent.app.data

import com.vent.app.data.settings.WindUnit
import com.vent.app.data.settings.WaveUnit
import com.vent.app.data.settings.TempUnit
import org.junit.Test
import org.junit.Assert.assertEquals

class UnitsFormatTest {

    @Test
    fun testFormatWindKnots() {
        assertEquals("12 kn", Units.format(12.0, WindUnit.KNOTS))
    }

    @Test
    fun testFormatWindKmh() {
        assertEquals("22.2 km/h", Units.format(12.0, WindUnit.KMH))
    }

    @Test
    fun testFormatWaveMeters() {
        assertEquals("12 m", Units.format(12.0, WaveUnit.METERS))
    }

    @Test
    fun testFormatTempCelsius() {
        assertEquals("12°", Units.format(12.0, TempUnit.CELSIUS))
    }
}