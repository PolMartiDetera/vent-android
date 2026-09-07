package com.vent.app.domain.alerts

import com.vent.app.data.model.ForecastHour
import com.vent.app.data.model.WeatherPoint

enum class AlertMetric(val displayUnit: String) {
    WIND_SPEED("kn"),
    WIND_GUST("kn"),
    WAVE_HEIGHT("m"),
}

enum class AlertDirection { ABOVE, BELOW }

enum class AlertSeverity { THRESHOLD, SEVERE }

/** A user-configured threshold rule (persisted when Settings lands in Task 11). */
data class AlertRule(
    val id: Long = 0,
    val metric: AlertMetric,
    val threshold: Double,
    val direction: AlertDirection = AlertDirection.ABOVE,
    val enabled: Boolean = true,
    val label: String = "",
)

data class ThresholdAlertResult(
    val rule: AlertRule,
    val peakValue: Double,
    val triggeredAt: Long,
    val severity: AlertSeverity,
    val message: String,
)

/**
 * Pure evaluation of threshold rules against the next-24h hourly forecast in a
 * [WeatherPoint]. No IO, no Android dependencies — unit-testable on the JVM.
 */
class ThresholdAlertEngine {

    fun evaluate(
        rules: List<AlertRule>,
        point: WeatherPoint,
        windowMs: Long = 24 * 3600_000L,
    ): List<ThresholdAlertResult> {
        val window = point.nextHours.filter { it.time in point.time until (point.time + windowMs) }
        if (window.isEmpty()) return emptyList()

        return rules.asSequence()
            .filter { it.enabled }
            .mapNotNull { rule ->
                val peak = peakHour(rule.metric, window) ?: return@mapNotNull null
                val value = sample(rule.metric, peak)
                if (crosses(rule, value)) {
                    ThresholdAlertResult(
                        rule = rule,
                        peakValue = value,
                        triggeredAt = peak.time,
                        severity = severity(rule, value),
                        message = message(rule, value),
                    )
                } else null
            }
            .toList()
    }

    private fun crosses(rule: AlertRule, value: Double): Boolean = when (rule.direction) {
        AlertDirection.ABOVE -> value >= rule.threshold
        AlertDirection.BELOW -> value <= rule.threshold
    }

    private fun severity(rule: AlertRule, value: Double): AlertSeverity =
        if (rule.direction == AlertDirection.ABOVE && value >= rule.threshold * 1.4) AlertSeverity.SEVERE
        else rule.metric.let { if (it == AlertMetric.WAVE_HEIGHT && value >= 3.0) AlertSeverity.SEVERE else AlertSeverity.THRESHOLD }

    private fun message(rule: AlertRule, value: Double): String {
        val unit = rule.metric.displayUnit
        val label = rule.label.ifBlank { rule.metric.name.lowercase().replace('_', ' ') }
        return when (rule.direction) {
            AlertDirection.ABOVE -> "$label forecast at ${value.round1()} $unit, above ${rule.threshold.round1()} $unit"
            AlertDirection.BELOW -> "$label forecast at ${value.round1()} $unit, below ${rule.threshold.round1()} $unit"
        }
    }

    private fun peakHour(metric: AlertMetric, window: List<ForecastHour>): ForecastHour? =
        when (metric) {
            AlertMetric.WIND_SPEED -> window.maxByOrNull { it.windSpeedKnots }
            AlertMetric.WIND_GUST -> window.maxByOrNull { it.windGustKnots }
            AlertMetric.WAVE_HEIGHT -> window.maxByOrNull { it.waveHeightMeters }
        }

    private fun sample(metric: AlertMetric, hour: ForecastHour): Double =
        when (metric) {
            AlertMetric.WIND_SPEED -> hour.windSpeedKnots
            AlertMetric.WIND_GUST -> hour.windGustKnots
            AlertMetric.WAVE_HEIGHT -> hour.waveHeightMeters
        }

    private fun Double.round1(): Double = (this * 10).toLong() / 10.0
}