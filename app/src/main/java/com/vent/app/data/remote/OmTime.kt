package com.vent.app.data.remote

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Open-Meteo returns wall-clock timestamps in the location's local timezone
 * (we request `timezone=auto`), plus `utc_offset_seconds`. Converting the local
 * string to true epoch millis is done by parsing as UTC and shifting by the
 * offset, so the result is correct regardless of the device's own timezone.
 */
object OmTime {
    private val formatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm")

    fun toEpochMillis(localTime: String, utcOffsetSeconds: Int): Long {
        if (localTime.isEmpty()) return 0L
        return runCatching {
            val ldt = LocalDateTime.parse(localTime, formatter)
            ldt.toEpochSecond(java.time.ZoneOffset.UTC) * 1000L + utcOffsetSeconds * 1000L
        }.getOrDefault(0L)
    }
}