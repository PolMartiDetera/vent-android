package com.vent.app.data.repo

import com.vent.app.data.settings.SettingsDataStore
import com.vent.app.data.settings.UserSettings
import kotlinx.coroutines.flow.Flow

class SettingsRepository(
    private val store: SettingsDataStore,
) {
    val settings: Flow<UserSettings> = store.settings

    suspend fun update(transform: UserSettings.() -> UserSettings) = store.update(transform)

    suspend fun setLocationId(id: Long?) = store.setLocationId(id)
}