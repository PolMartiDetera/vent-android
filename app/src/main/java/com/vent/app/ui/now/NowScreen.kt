package com.vent.app.ui.now

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vent.app.data.model.WeatherPoint
import com.vent.app.ui.components.BriefCard
import com.vent.app.ui.components.EphemerisCard
import com.vent.app.ui.components.FishingCard
import com.vent.app.ui.components.HeroCard
import com.vent.app.ui.components.HourlyStrip
import com.vent.app.ui.components.NowcastCard
import org.koin.androidx.compose.koinViewModel

/**
 * Now (Home) tab. Wind-first scrolling column of weather cards, with the huge
 * wind hero at the top (spec order: hero > brief > fishing > nowcast > ephemeris > hourly).
 */
@Composable
fun NowScreen(
    onOpenCompass: () -> Unit,
    viewModel: NowViewModel = koinViewModel(),
) {
    val uiState by viewModel.ui.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { viewModel.nowcast() }

    Surface(modifier = Modifier.fillMaxSize()) {
        when {
            uiState.loading && uiState.weather == null -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }

            uiState.weather != null -> {
                NowContent(
                    uiState = uiState,
                    onOpenCompass = onOpenCompass,
                )
            }

            else -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = uiState.error ?: "Nothing here yet",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun NowContent(
    uiState: NowUiState,
    onOpenCompass: () -> Unit,
) {
    val point = uiState.weather ?: return
    val settings = uiState.settings

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = WindowInsets.safeDrawing.asPaddingValues().calculateTopPadding() + 8.dp,
            bottom = WindowInsets.safeDrawing.asPaddingValues().calculateBottomPadding() + 24.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item(key = "place") {
            Text(
                text = uiState.placeName,
                style = MaterialTheme.typography.titleLarge,
            )
        }
        item(key = "hero") {
            HeroCard(
                speedKn = point.wind.speedKnots,
                gustKn = point.wind.gustKnots,
                directionDeg = point.wind.directionDeg,
                settings = settings,
                onOpenCompass = onOpenCompass,
            )
        }
        item(key = "brief") {
            if (uiState.brief.isNotBlank()) {
                BriefCard(brief = uiState.brief)
            }
        }
        item(key = "fishing") {
            uiState.fishing?.let { FishingCard(conditions = it) }
        }
        item(key = "nowcast") {
            NowcastCard(
                hours = point.nextHours,
                settings = settings,
            )
        }
        item(key = "ephemeris") {
            EphemerisCard(
                sunrise = point.sunrise,
                sunset = point.sunset,
                moonrise = point.moonrise,
                moonset = point.moonset,
                moonPhase = point.moonPhase,
            )
        }
        item(key = "hourly-title") {
            Row(Modifier.fillMaxWidth()) {
                Text("Next 24 hours", style = MaterialTheme.typography.titleMedium)
            }
        }
        item(key = "hourly") {
            HourlyStrip(
                hours = point.nextHours.take(24),
                settings = settings,
            )
        }
    }
}