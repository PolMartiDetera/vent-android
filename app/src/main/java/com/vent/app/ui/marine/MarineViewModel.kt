package com.vent.app.ui.marine

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vent.app.data.model.ForecastDay
import com.vent.app.data.model.ForecastHour
import com.vent.app.data.model.WeatherPoint
import com.vent.app.data.repo.LocationRepository
import com.vent.app.data.repo.WeatherRepository
import com.vent.app.domain.alerts.AlertMetric
import com.vent.app.domain.alerts.AlertRule
import com.vent.app.domain.alerts.ThresholdAlertEngine
import com.vent.app.domain.alerts.ThresholdAlertResult
import java.time.Instant
import java.time.ZoneId
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class MarineUiState(
    val loading: Boolean = true,
    val weather: WeatherPoint? = null,
    val placeName: String = "",
    val alerts: List<ThresholdAlertResult> = emptyList(),
    val days: List<ForecastDay> = emptyList(),
    val error: String? = null,
)

/** Marine hub: compass reading, threshold warnings, forecast days + chart series. */
class MarineViewModel(
    private val weatherRepo: WeatherRepository,
    private val locationRepo: LocationRepository,
    private val engine: ThresholdAlertEngine,
) : ViewModel() {

    private val _ui = MutableStateFlow(MarineUiState())
    val ui: StateFlow<MarineUiState> = _ui.asStateFlow()

    fun load() {
        viewModelScope.launch {
            _ui.value = MarineUiState(loading = true)
            val loc = locationRepo.default() ?: locationRepo.observeAll().first().firstOrNull()
            if (loc == null) {
                _ui.value = MarineUiState(loading = false, error = "No saved location yet.")
                return@launch
            }
            _ui.value = _ui.value.copy(placeName = loc.name)
            weatherRepo.weatherFor(loc.lat, loc.lon).collectLatest { point ->
                _ui.value = MarineUiState(
                    loading = false,
                    weather = point,
                    placeName = loc.name,
                    alerts = engine.evaluate(DEFAULT_RULES, point),
                    days = splitDays(point.nextHours),
                )
            }
        }
    }

    companion object {
        /** Sensible out-of-the-box thresholds; user-editable rules arrive with Settings (Task 11). */
        val DEFAULT_RULES = listOf(
            AlertRule(metric = AlertMetric.WIND_GUST, threshold = 17.0, label = "Gusts"),
            AlertRule(metric = AlertMetric.WAVE_HEIGHT, threshold = 2.0, label = "Waves"),
            AlertRule(metric = AlertMetric.WIND_SPEED, threshold = 25.0, label = "Wind"),
        )
    }
}

/** Bucket the available forecast window into per-day summaries, oldest first. */
internal fun splitDays(hours: List<ForecastHour>): List<ForecastDay> {
    val zone = ZoneId.systemDefault()
    return hours.groupBy { Instant.ofEpochMilli(it.time).atZone(zone).toLocalDate() }
        .toSortedMap()
        .map { (date, list) ->
            ForecastDay(
                date = date.atStartOfDay(zone).toInstant().toEpochMilli(),
                windMaxKnots = list.maxOf { it.windSpeedKnots },
                gustMaxKnots = list.maxOf { it.windGustKnots },
                waveMaxMeters = list.maxOf { it.waveHeightMeters },
                wavePeriodSeconds = list.maxByOrNull { it.waveHeightMeters }?.wavePeriodSeconds ?: 0.0,
                tempMinCelsius = list.minOf { it.tempCelsius },
                tempMaxCelsius = list.maxOf { it.tempCelsius },
                precipMm = list.sumOf { it.precipMm },
                windDirectionDeg = list.maxByOrNull { it.windSpeedKnots }?.windDirectionDeg ?: 0,
                hours = list,
            )
        }
}