package com.vent.app.ui.marine

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vent.app.domain.alerts.AlertSeverity
import com.vent.app.domain.alerts.ThresholdAlertResult
import com.vent.app.haptic.HapticController
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import org.koin.core.context.GlobalContext

private enum class MarineSection(val chipLabel: String, val index: Int) {
    COMPASS("Compass", 0),
    WARNINGS("Warnings", 1),
    FORECAST("Forecast", 2),
}

/** Marine hub: pinned warnings banner + scroll-spy chips over compass/warnings/forecast. */
@Composable
fun MarineScreen(focus: String?, modifier: Modifier = Modifier) {
    val viewModel: MarineViewModel = koinViewModel()
    val uiState by viewModel.ui.collectAsStateWithLifecycle()
    val haptics: HapticController = remember { GlobalContext.get().get<HapticController>() }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    var expandedDate by rememberSaveable { mutableStateOf<Long?>(null) }

    LaunchedEffect(Unit) { viewModel.load() }
    LaunchedEffect(focus) {
        val target = MarineSection.entries.firstOrNull { it.name.equals(focus, ignoreCase = true) }
            ?.index ?: return@LaunchedEffect
        listState.animateScrollToItem(target)
    }

    Column(modifier = modifier.fillMaxSize()) {
        WarningsBanner(uiState.alerts)
        SectionChips(
            selectedIndex = when {
                listState.firstVisibleItemIndex >= MarineSection.FORECAST.index -> MarineSection.FORECAST.index
                listState.firstVisibleItemIndex == MarineSection.WARNINGS.index -> MarineSection.WARNINGS.index
                else -> MarineSection.COMPASS.index
            },
            onSelect = { section ->
                haptics.reveal()
                scope.launch { listState.animateScrollToItem(section.index) }
            },
        )

        val error = uiState.error
        when {
            uiState.loading && uiState.weather == null -> Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) { CircularProgressIndicator() }

            error != null -> Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = error,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(24.dp),
                )
            }

            else -> LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                item(key = "compass") { CompassSection(uiState, haptics) }
                item(key = "warnings") { WarningsSection(uiState.alerts) }
                item(key = "forecast") {
                    ForecastSection(
                        days = uiState.days,
                        hours = uiState.weather?.nextHours.orEmpty(),
                        expandedDate = expandedDate,
                        onDayClick = { date ->
                            expandedDate = if (expandedDate == date) null else date
                            haptics.reveal()
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun CompassSection(uiState: MarineUiState, haptics: HapticController) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        val weather = uiState.weather
        if (weather != null) {
            Text(
                text = uiState.placeName,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.semantics { heading() },
            )
            Text(
                text = "Wind out of the ${com.vent.app.data.model.WindRose.cardinalSpoken(weather.wind.directionDeg)}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            CompassRose(
                windDirectionDeg = weather.wind.directionDeg,
                speedKnots = weather.wind.speedKnots,
                gustKnots = weather.wind.gustKnots,
                haptics = haptics,
                modifier = Modifier.fillMaxWidth().aspectRatio(1f),
            )
        }
    }
}

@Composable
private fun WarningsBanner(alerts: List<ThresholdAlertResult>) {
    val severe = alerts.any { it.severity == AlertSeverity.SEVERE }
    val container = if (severe) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.tertiaryContainer
    val content = if (severe) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onTertiaryContainer
    Surface(color = container, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Filled.Warning,
                contentDescription = null,
                tint = content,
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.width(10.dp))
            Text(
                text = when (alerts.size) {
                    0 -> "No warnings in the next 24 h"
                    1 -> "1 warning in the next 24 h"
                    else -> "${alerts.size} warnings in the next 24 h"
                },
                style = MaterialTheme.typography.labelLarge,
                color = content,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun SectionChips(selectedIndex: Int, onSelect: (MarineSection) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        MarineSection.entries.forEach { section ->
            FilterChip(
                selected = selectedIndex == section.index,
                onClick = { onSelect(section) },
                label = { Text(section.chipLabel) },
            )
        }
    }
}