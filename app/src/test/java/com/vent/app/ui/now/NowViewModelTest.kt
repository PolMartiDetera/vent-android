package com.vent.app.ui.now

import com.vent.app.data.db.LocationDao
import com.vent.app.data.db.LocationEntity
import com.vent.app.data.db.WeatherCacheDao
import com.vent.app.data.db.WeatherCacheEntity
import com.vent.app.data.model.DailyInfo
import com.vent.app.data.model.ForecastHour
import com.vent.app.data.provider.WaveProvider
import com.vent.app.data.provider.WindProvider
import com.vent.app.data.repo.LocationRepository
import com.vent.app.data.repo.SettingsRepository
import com.vent.app.data.repo.WeatherRepository
import com.vent.app.data.settings.SettingsDataStore
import com.vent.app.data.settings.UserSettings
import com.vent.app.data.settings.WindUnit
import com.vent.app.domain.brief.DayBriefGenerator
import com.vent.app.domain.brief.FishingConditionsGenerator
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class NowViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private val location = LocationEntity(id = 1, name = "Marseille", lat = 43.29, lon = 5.38, isDefault = true, sortOrder = 0)

    private fun fakeLocationDao(): LocationDao = object : LocationDao {
        override fun observeAll(): Flow<List<LocationEntity>> = flowOf(listOf(location))
        override suspend fun get(id: Long): LocationEntity? = location
        override suspend fun getDefault(): LocationEntity? = location
        override suspend fun insert(entity: LocationEntity): Long = 2
        override suspend fun update(entity: LocationEntity) {}
        override suspend fun delete(entity: LocationEntity) {}
    }

    private fun fakeCacheDao(): WeatherCacheDao = object : WeatherCacheDao {
        override suspend fun get(id: String): WeatherCacheEntity? = null
        override suspend fun upsert(entity: WeatherCacheEntity) {}
        override suspend fun deleteOlderThan(cutoff: Long) {}
    }

    private fun fakeSettingsStore(): SettingsDataStore {
        val file = File.createTempFile("vent-settings", ".preferences_pb")
        file.delete()
        val dataStore = PreferenceDataStoreFactory.create(scope = CoroutineScope(dispatcher + Job())) {
            file
        }
        return SettingsDataStore(dataStore)
    }

    private val base = 1_700_000_000_000L

    private fun fakeWindProvider(): WindProvider = object : WindProvider {
        override suspend fun wind(lat: Double, lon: Double, time: Long): List<ForecastHour> =
            (0..47).map { k ->
                ForecastHour(
                    time = time + k * 3600_000L,
                    windSpeedKnots = 12.0,
                    windGustKnots = 18.0,
                    windDirectionDeg = 45,
                    waveHeightMeters = 0.4,
                    wavePeriodSeconds = 6.0,
                    tempCelsius = 20.0,
                    precipMm = 0.0,
                    pressureHpa = 1013.0 + k * 0.1,
                    cloudCoverPct = 10,
                )
            }

        override suspend fun current(lat: Double, lon: Double, time: Long): ForecastHour =
            wind(lat, lon, time).first()

        override suspend fun daily(lat: Double, lon: Double): DailyInfo =
            DailyInfo(sunrise = base, sunset = base + 12 * 3600_000L, moonrise = base + 2, moonset = base + 3, moonPhase = 0.5)
    }

    private fun fakeWaveProvider(): WaveProvider = object : WaveProvider {
        override suspend fun wave(lat: Double, lon: Double, time: Long): List<ForecastHour> =
            (0..47).map { k ->
                ForecastHour(
                    time = time + k * 3600_000L,
                    windSpeedKnots = 0.0,
                    windGustKnots = 0.0,
                    windDirectionDeg = 0,
                    waveHeightMeters = 0.4,
                    wavePeriodSeconds = 6.0,
                    tempCelsius = 0.0,
                    precipMm = 0.0,
                    pressureHpa = 0.0,
                    cloudCoverPct = 0,
                )
            }
    }

    private fun buildViewModel(): NowViewModel {
        val repo = WeatherRepository(fakeWindProvider(), fakeWaveProvider(), fakeCacheDao())
        return NowViewModel(
            weatherRepo = repo,
            locationRepo = LocationRepository(fakeLocationDao()),
            settingsRepo = SettingsRepository(fakeSettingsStore()),
            briefGen = DayBriefGenerator(),
            fishingGen = FishingConditionsGenerator(),
        )
    }

    @Test
    fun `load renders wind-first state with brief and fishing conditions`() = runTest(dispatcher) {
        val vm = buildViewModel()
        val state = vm.ui.first { !it.loading }

        assertEquals("Marseille", state.placeName)
        assertNull(state.error)
        assertEquals(12.0, state.weather?.wind?.speedKnots ?: 0.0, 0.0)
        assertTrue(state.brief.contains("moderate breeze"))
        assertTrue(state.fishing?.summary?.lowercase()?.contains("breeze") == true)
        assertTrue(state.fishing?.effects?.isNotBlank() == true)
        assertTrue(state.fishing?.method?.isNotBlank() == true)
    }

    @Test
    fun `state exposes settings for display units`() = runTest(dispatcher) {
        val vm = buildViewModel()
        val state = vm.ui.first { !it.loading }

        assertEquals(WindUnit.KNOTS, state.settings.windUnit)
    }

    @Test
    fun `no location shows friendly error and no weather`() = runTest(dispatcher) {
        val noLocDao = object : LocationDao {
            override fun observeAll(): Flow<List<LocationEntity>> = flowOf(emptyList())
            override suspend fun get(id: Long): LocationEntity? = null
            override suspend fun getDefault(): LocationEntity? = null
            override suspend fun insert(entity: LocationEntity): Long = 1
            override suspend fun update(entity: LocationEntity) {}
            override suspend fun delete(entity: LocationEntity) {}
        }
        val vm = NowViewModel(
            weatherRepo = WeatherRepository(fakeWindProvider(), fakeWaveProvider(), fakeCacheDao()),
            locationRepo = LocationRepository(noLocDao),
            settingsRepo = SettingsRepository(fakeSettingsStore()),
            briefGen = DayBriefGenerator(),
            fishingGen = FishingConditionsGenerator(),
        )

        val state = vm.ui.first { !it.loading }
        assertNull(state.weather)
        assertTrue(state.error?.contains("Add a location") == true)
    }
}