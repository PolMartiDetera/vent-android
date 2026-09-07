package com.vent.app.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [WeatherCacheEntity::class, LocationEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class VentDatabase : RoomDatabase() {
    abstract fun weatherCacheDao(): WeatherCacheDao
    abstract fun locationDao(): LocationDao

    companion object {
        fun build(context: Context): VentDatabase =
            Room.databaseBuilder(context, VentDatabase::class.java, "vent.db")
                .fallbackToDestructiveMigration(dropAllTables = true)
                .build()
    }
}