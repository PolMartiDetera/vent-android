package com.vent.app.data.repo

import com.vent.app.data.db.WeatherCacheDao
import com.vent.app.data.db.WeatherCacheEntity
import com.vent.app.data.model.ForecastDay
import com.vent.app.data.model.ForecastHour
import com.vent.app.data.model.WaveData
import com.vent.app.data.model.WeatherPoint
import com.vent.app.data.model.WindData
import com.vent.app.data.model.WindRose
import com.vent.app.data.provider.WaveProvider
import com.vent.app.data.provider.WindProvider
import java.time.Instant
import java.time.ZoneId
import java.util.Locale
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.json.Json
import kotlin.math.abs

/**
 * Fetches from the providers, merges wind + wave into hourly [ForecastHour]s,
 * assembles a [WeatherPoint], and caches it to Room. On network failure it emits
 * the last cached point instead of crashing the UI.
 */
class WeatherRepository(
    private val windProvider: WindProvider,
    private val waveProvider: WaveProvider,
    private val cacheDao: WeatherCacheDao,
    private val now: () -> Long = System::currentTimeMillis,
) {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    fun weatherFor(lat: Double, lon: Double): Flow<WeatherPoint> = flow {
        val cached = cached(lat, lon)
        if (cached != null) emit(cached)
        try {
            emit(refresh(lat, lon))
        } catch (e: Exception) {
            if (cached == null) throw e
            // keep serving stale cache; callers surface freshness via updatedAt
        }
    }

    suspend fun refresh(lat: Double, lon: Double): WeatherPoint {
        val time = now()
        val wind = windProvider.wind(lat, lon, time)
        val wave = waveProvider.wave(lat, lon, time)
        val currentSource = windProvider.current(lat, lon, time)
        val daily = windProvider.daily(lat, lon)

        val merged = mergeHourly(wind, wave)
        val current = merged.firstOrNull { abs(it.time - time) <= 3600_000L } ?: currentSource

        val point = WeatherPoint(
            lat = lat,
            lon = lon,
            time = time,
            wind = WindData(
                speedKnots = current.windSpeedKnots,
                gustKnots = current.windGustKnots,
                directionDeg = current.windDirectionDeg,
                cardinal = WindRose.cardinal(current.windDirectionDeg),
            ),
            // wave provider stores its direction in the shared hour slot
            wave = if (current.waveHeightMeters <= 0.0) null else WaveData(
                heightMeters = current.waveHeightMeters,
                periodSeconds = current.wavePeriodSeconds,
                directionDeg = current.windDirectionDeg,
            ),
            tempCelsius = current.tempCelsius,
            pressureHpa = current.pressureHpa,
            cloudCoverPct = current.cloudCoverPct,
            sunrise = daily.sunrise,
            sunset = daily.sunset,
            moonrise = daily.moonrise,
            moonset = daily.moonset,
            moonPhase = daily.moonPhase,
            day = buildDay(merged, time),
            nextHours = merged,
        )

        cacheDao.upsert(
            WeatherCacheEntity(
                id = cacheId(lat, lon),
                lat = lat,
                lon = lon,
                json = json.encodeToString(WeatherPoint.serializer(), point),
                updatedAt = time,
            )
        )
        return point
    }

    suspend fun cached(lat: Double, lon: Double): WeatherPoint? =
        cacheDao.get(cacheId(lat, lon))?.json?.takeIf { it.isNotEmpty() }
            ?.let { runCatching { json.decodeFromString(WeatherPoint.serializer(), it) }.getOrNull() }

    private fun cacheId(lat: Double, lon: Double): String =
        String.format(Locale.US, "%.6f,%.6f", lat, lon)
}

/** Merge wind + wave hourly series bucket-aligned by hour. */
internal fun mergeHourly(wind: List<ForecastHour>, wave: List<ForecastHour>): List<ForecastHour> {
    val waveByHour = wave.associateBy { it.time }
    return wind.mapNotNull { w ->
        val wa = waveByHour[w.time]
        ForecastHour(
            time = w.time,
            windSpeedKnots = w.windSpeedKnots,
            windGustKnots = w.windGustKnots,
            windDirectionDeg = w.windDirectionDeg,
            waveHeightMeters = wa?.waveHeightMeters ?: 0.0,
            wavePeriodSeconds = wa?.wavePeriodSeconds ?: 0.0,
            tempCelsius = w.tempCelsius,
            precipMm = w.precipMm,
            pressureHpa = w.pressureHpa,
            cloudCoverPct = w.cloudCoverPct,
        )
    }.sortedBy { it.time }
}

/** Aggregate the next day of hours into a [ForecastDay] (local-time day bucket). */
internal fun buildDay(hours: List<ForecastHour>, time: Long): ForecastDay? {
    val zone = ZoneId.systemDefault()
    val startOfDay = Instant.ofEpochMilli(time).atZone(zone).toLocalDate()
        .atStartOfDay(zone).toInstant().toEpochMilli()
    val dayHours = hours.filter { it.time in startOfDay until (startOfDay + 86_400_000L) }
    if (dayHours.isEmpty()) return null
    return ForecastDay(
        date = startOfDay,
        windMaxKnots = dayHours.maxOf { it.windSpeedKnots },
        gustMaxKnots = dayHours.maxOf { it.windGustKnots },
        waveMaxMeters = dayHours.maxOf { it.waveHeightMeters },
        wavePeriodSeconds = dayHours.maxByOrNull { it.waveHeightMeters }?.wavePeriodSeconds ?: 0.0,
        tempMinCelsius = dayHours.minOf { it.tempCelsius },
        tempMaxCelsius = dayHours.maxOf { it.tempCelsius },
        precipMm = dayHours.sumOf { it.precipMm },
        windDirectionDeg = dayHours.maxByOrNull { it.windSpeedKnots }?.windDirectionDeg ?: 0,
        hours = dayHours,
    )
}