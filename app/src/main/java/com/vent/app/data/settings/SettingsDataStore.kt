package com.vent.app.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

/** Backs [UserSettings] with single DataStore preferences file. */
class SettingsDataStore(
    private val store: DataStore<Preferences>,
) {
    val settings: Flow<UserSettings> = store.data.map { it.toSettings() }

    suspend fun update(transform: UserSettings.() -> UserSettings) {
        val next = settings.first()
        store.edit { prefs -> next.transform().writeTo(prefs) }
    }

    suspend fun setLocationId(id: Long?) {
        store.edit { prefs ->
            if (id == null) prefs.remove(LOCATION_ID) else prefs[LOCATION_ID] = id
        }
    }
}

private fun Preferences.toSettings(): UserSettings = UserSettings(
    windUnit = enumOrDefault(this[WIND_UNIT], WindUnit.KNOTS),
    waveUnit = enumOrDefault(this[WAVE_UNIT], WaveUnit.METERS),
    tempUnit = enumOrDefault(this[TEMP_UNIT], TempUnit.CELSIUS),
    updateFreqMin = this[UPDATE_FREQ_MIN] ?: 60,
    windSource = this[WIND_SOURCE] ?: "openmeteo",
    waveSource = this[WAVE_SOURCE] ?: "openmeteo",
    tideSource = this[TIDE_SOURCE] ?: "none",
    dailyBrief = this[DAILY_BRIEF] ?: true,
    thresholdAlerts = this[THRESHOLD_ALERTS] ?: true,
    darkTheme = this[DARK_THEME] ?: false,
    useSystemTheme = this[USE_SYSTEM_THEME] ?: true,
    dynamicColor = this[DYNAMIC_COLOR] ?: true,
    haptics = this[HAPTICS] ?: true,
    language = this[LANGUAGE] ?: "system",
    locationId = this[LOCATION_ID],
)

private fun UserSettings.writeTo(prefs: androidx.datastore.preferences.core.MutablePreferences) {
    prefs[WIND_UNIT] = windUnit.name
    prefs[WAVE_UNIT] = waveUnit.name
    prefs[TEMP_UNIT] = tempUnit.name
    prefs[UPDATE_FREQ_MIN] = updateFreqMin
    prefs[WIND_SOURCE] = windSource
    prefs[WAVE_SOURCE] = waveSource
    prefs[TIDE_SOURCE] = tideSource
    prefs[DAILY_BRIEF] = dailyBrief
    prefs[THRESHOLD_ALERTS] = thresholdAlerts
    prefs[DARK_THEME] = darkTheme
    prefs[USE_SYSTEM_THEME] = useSystemTheme
    prefs[DYNAMIC_COLOR] = dynamicColor
    prefs[HAPTICS] = haptics
    prefs[LANGUAGE] = language
    if (locationId == null) prefs.remove(LOCATION_ID) else prefs[LOCATION_ID] = locationId
}

private inline fun <reified T : Enum<T>> enumOrDefault(value: String?, default: T): T =
    value?.let { runCatching { enumValueOf<T>(it) }.getOrNull() } ?: default

val WIND_UNIT = stringPreferencesKey("wind_unit")
val WAVE_UNIT = stringPreferencesKey("wave_unit")
val TEMP_UNIT = stringPreferencesKey("temp_unit")
val UPDATE_FREQ_MIN = intPreferencesKey("update_freq_min")
val WIND_SOURCE = stringPreferencesKey("wind_source")
val WAVE_SOURCE = stringPreferencesKey("wave_source")
val TIDE_SOURCE = stringPreferencesKey("tide_source")
val DAILY_BRIEF = booleanPreferencesKey("daily_brief")
val THRESHOLD_ALERTS = booleanPreferencesKey("threshold_alerts")
val DARK_THEME = booleanPreferencesKey("dark_theme")
val USE_SYSTEM_THEME = booleanPreferencesKey("use_system_theme")
val DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
val HAPTICS = booleanPreferencesKey("haptics")
val LANGUAGE = stringPreferencesKey("language")
val LOCATION_ID = longPreferencesKey("location_id")