package com.vent.app.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface WeatherCacheDao {
    @Query("SELECT * FROM weather_cache WHERE id = :id")
    suspend fun get(id: String): WeatherCacheEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: WeatherCacheEntity)

    @Query("DELETE FROM weather_cache WHERE updatedAt < :cutoff")
    suspend fun deleteOlderThan(cutoff: Long)
}

@Dao
interface LocationDao {
    @Query("SELECT * FROM locations ORDER BY sortOrder")
    fun observeAll(): Flow<List<LocationEntity>>

    @Query("SELECT * FROM locations WHERE id = :id")
    suspend fun get(id: Long): LocationEntity?

    @Query("SELECT * FROM locations WHERE isDefault = 1 LIMIT 1")
    suspend fun getDefault(): LocationEntity?

    @Insert
    suspend fun insert(entity: LocationEntity): Long

    @Update
    suspend fun update(entity: LocationEntity)

    @Delete
    suspend fun delete(entity: LocationEntity)
}