package com.vent.app.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/** One cached hourly set per location (keyed by "lat,lon"); upsert keeps the latest. */
@Entity(tableName = "weather_cache")
data class WeatherCacheEntity(
    @PrimaryKey val id: String,
    val lat: Double,
    val lon: Double,
    val json: String,
    val updatedAt: Long,
)

/** User-saved forecast locations. */
@Entity(tableName = "locations")
data class LocationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val lat: Double,
    val lon: Double,
    val isDefault: Boolean = false,
    val sortOrder: Int = 0,
)