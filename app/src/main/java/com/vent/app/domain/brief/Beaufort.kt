package com.vent.app.domain.brief

/** Beaufort-scale descriptors keyed by mean wind speed in knots. */
object Beaufort {
    fun descriptor(knots: Double): String = when {
        knots >= 64 -> "hurricane-force"
        knots >= 48 -> "violent storm"
        knots >= 41 -> "storm"
        knots >= 34 -> "gale"
        knots >= 28 -> "near-gale"
        knots >= 22 -> "strong breeze"
        knots >= 17 -> "fresh breeze"
        knots >= 11 -> "moderate breeze"
        knots >= 7 -> "gentle breeze"
        knots >= 4 -> "light breeze"
        knots >= 1 -> "light air"
        else -> "calm"
    }

    fun seaState(meters: Double): String = when {
        meters >= 9.0 -> "phenomenal"
        meters >= 6.0 -> "very high"
        meters >= 4.0 -> "high"
        meters >= 2.5 -> "rough"
        meters >= 1.25 -> "moderate"
        meters >= 0.5 -> "slight"
        else -> "calm"
    }
}