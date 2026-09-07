package com.vent.app.di

import com.vent.app.data.db.VentDatabase
import com.vent.app.data.notify.NotificationHelper
import com.vent.app.data.provider.NoopTideProvider
import com.vent.app.data.provider.OpenMeteoWaveProvider
import com.vent.app.data.provider.OpenMeteoWindProvider
import com.vent.app.data.provider.TideProvider
import com.vent.app.data.provider.WaveProvider
import com.vent.app.data.provider.WindProvider
import com.vent.app.data.remote.OpenMeteoClient
import com.vent.app.data.repo.LocationRepository
import com.vent.app.data.repo.SettingsRepository
import com.vent.app.data.repo.WeatherRepository
import com.vent.app.data.settings.SettingsDataStore
import com.vent.app.data.settings.settingsDataStore
import com.vent.app.domain.alerts.ThresholdAlertEngine
import com.vent.app.domain.brief.DayBriefGenerator
import com.vent.app.domain.brief.FishingConditionsGenerator
import com.vent.app.haptic.HapticController
import com.vent.app.ui.marine.MarineViewModel
import com.vent.app.ui.now.NowViewModel
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    // --- Network -----------------------------------------------------------------
    single {
        HttpClient(OkHttp) {
            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                    coerceInputValues = true
                })
            }
        }
    }
    single { OpenMeteoClient(get()) }

    // --- Providers ---------------------------------------------------------------
    single<WindProvider> { OpenMeteoWindProvider(get()) }
    single<WaveProvider> { OpenMeteoWaveProvider(get()) }
    single<TideProvider> { NoopTideProvider() }

    // --- Persistence ---------------------------------------------------------------
    single { VentDatabase.build(androidContext()) }
    single { get<VentDatabase>().weatherCacheDao() }
    single { get<VentDatabase>().locationDao() }
    single { SettingsDataStore(androidContext().settingsDataStore) }

    // --- Repositories --------------------------------------------------------------
    single { WeatherRepository(get(), get(), get()) }
    single { LocationRepository(get()) }
    single { SettingsRepository(get()) }

    // --- Notifications -------------------------------------------------------------
    single { NotificationHelper(androidContext()) }

    // --- Haptics --------------------------------------------------------------------
    single { HapticController(androidContext(), get()) }

    // --- Domain services -----------------------------------------------------------
    single { DayBriefGenerator() }
    single { FishingConditionsGenerator() }
    single { ThresholdAlertEngine() }

    // --- ViewModels (added per-screen as screens land) --------------------------------
    viewModel { NowViewModel(get(), get(), get(), get(), get()) }
    viewModel { MarineViewModel(get(), get(), get()) }
}