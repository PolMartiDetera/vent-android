package com.vent.app.ui.now

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vent.app.data.model.WeatherPoint
import com.vent.app.data.repo.LocationRepository
import com.vent.app.data.repo.SettingsRepository
import com.vent.app.data.repo.WeatherRepository
import com.vent.app.data.settings.UserSettings
import com.vent.app.domain.brief.DayBriefGenerator
import com.vent.app.domain.brief.FishingConditions
import com.vent.app.domain.brief.FishingConditionsGenerator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class NowUiState(
    val loading: Boolean = true,
    val weather: WeatherPoint? = null,
    val placeName: String = "",
    val brief: String = "",
    val fishing: FishingConditions? = null,
    val settings: UserSettings = UserSettings(),
    val error: String? = null,
)

/**
 * Backs the Now (Home) screen. Watches location + settings, feeds the merged
 * WeatherPoint through the two plain-language generators, and exposes a StateFlow.
 */
class NowViewModel(
    private val weatherRepo: WeatherRepository,
    private val locationRepo: LocationRepository,
    private val settingsRepo: SettingsRepository,
    private val briefGen: DayBriefGenerator,
    private val fishingGen: FishingConditionsGenerator,
) : ViewModel() {

    private val _ui = MutableStateFlow(NowUiState())
    val ui: StateFlow<NowUiState> = _ui.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            val loc = locationRepo.default() ?: locationRepo.observeAll().first().firstOrNull()
            if (loc == null) {
                _ui.value = NowUiState(loading = false, error = "Add a location in Settings to get a forecast")
                return@launch
            }
            _ui.value = _ui.value.copy(loading = true, placeName = loc.name)
            combine(
                weatherRepo.weatherFor(loc.lat, loc.lon),
                settingsRepo.settings,
            ) { point, settings -> point to settings }
                .collectLatest { (point, settings) ->
                    _ui.value = render(point, loc.name, settings)
                }
        }
    }

    /** Pull fresh provider data (cache-and-refresh already emits cached first). */
    fun refresh() = load()

    /** Re-render current data (used after returning to the tab). */
    fun nowcast() {
        val point = _ui.value.weather ?: return
        _ui.value = render(point, _ui.value.placeName, _ui.value.settings)
    }

    private fun render(point: WeatherPoint, placeName: String, settings: UserSettings): NowUiState =
        NowUiState(
            loading = false,
            weather = point,
            placeName = placeName,
            brief = point.day?.let { briefGen.brief(it) }.orEmpty(),
            fishing = point.day?.let { day ->
                fishingGen.conditions(
                    day = day,
                    wind = point.wind,
                    wave = point.wave,
                    pressureTrend = pressureTrend(point),
                    moonPhase = point.moonPhase,
                    tide = null,
                )
            },
            settings = settings,
            error = null,
        )

    private fun pressureTrend(point: WeatherPoint): Double {
        val hours = point.nextHours
        if (hours.size < 2) return 0.0
        return hours.last().pressureHpa - hours.first().pressureHpa
    }
}