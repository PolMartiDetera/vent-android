package com.vent.app.data.settings

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.vent.app.data.repo.SettingsRepository
import java.io.File
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsRepositoryTest {

    @Test
    fun `default settings are returned when no preference has been written`() = runTest {
        val file = File.createTempFile("vent-settings-test", ".preferences_pb")
        file.deleteOnExit()
        val dataStore = PreferenceDataStoreFactory.create(produceFile = { file })
        val repo = SettingsRepository(SettingsDataStore(dataStore))

        val s = repo.settings.first()

        assertEquals(WindUnit.KNOTS, s.windUnit)
        assertEquals(WaveUnit.METERS, s.waveUnit)
        assertEquals(TempUnit.CELSIUS, s.tempUnit)
        assertEquals(60, s.updateFreqMin)
        assertEquals("openmeteo", s.windSource)
        assertEquals("openmeteo", s.waveSource)
        assertEquals("none", s.tideSource)
        assertTrue(s.dailyBrief)
        assertTrue(s.thresholdAlerts)
        assertTrue(s.dynamicColor)
        assertTrue(s.haptics)
        assertNull(s.locationId)
    }

    @Test
    fun `update persists and readback reflects the change`() = runTest {
        val file = File.createTempFile("vent-settings-test", ".preferences_pb")
        file.deleteOnExit()
        val dataStore = PreferenceDataStoreFactory.create(produceFile = { file })
        val repo = SettingsRepository(SettingsDataStore(dataStore))

        repo.update { copy(windUnit = WindUnit.KMH, thresholdAlerts = false, updateFreqMin = 30) }

        val s = repo.settings.first()
        assertEquals(WindUnit.KMH, s.windUnit)
        assertEquals(false, s.thresholdAlerts)
        assertEquals(30, s.updateFreqMin)
    }
}