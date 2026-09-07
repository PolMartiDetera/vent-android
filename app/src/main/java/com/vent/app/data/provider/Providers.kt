package com.vent.app.data.provider

import com.vent.app.data.model.ForecastHour
import com.vent.app.data.model.TideData

/**
 * Provider abstractions decouple the domain layer from specific forecast sources.
 * All methods return raw forecast series; repositories merge/cache them.
 */
interface WindProvider {
    /** Hourly forecast starting at [time]. Speeds in knots internally. */
    suspend fun wind(lat: Double, lon: Double, time: Long): List<ForecastHour>

    /** Current conditions snapshot. */
    suspend fun current(lat: Double, lon: Double, time: Long): ForecastHour
}

interface WaveProvider {
    /** Hourly wave forecast starting at [time]. Weather fields are zero-filled. */
    suspend fun wave(lat: Double, lon: Double, time: Long): List<ForecastHour>
}

interface TideProvider {
    suspend fun tide(lat: Double, lon: Double): TideData
}