package com.vent.app.haptic

import android.content.Context
import android.os.VibrationEffect
import android.os.Vibrator
import com.vent.app.data.repo.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

/**
 * Central haptic feedback. Every effect is gated by the user's haptics setting
 * (injected via [SettingsRepository]) and degrades to a no-op when unavailable.
 */
class HapticController(
    context: Context,
    settings: SettingsRepository,
) {
    private val vibrator: Vibrator? = context.getSystemService(Vibrator::class.java)
    private val works = Vibrator::class.java
        .let { v ->
            vibrator?.let { it.hasVibrator() } == true
        }

    @Volatile
    private var enabled = true

    companion object {
        private const val TICK_MS = 8L
        private const val REVEAL_MS = 28L
        private val WARNING_PATTERN = longArrayOf(0, 60, 40, 60)
        private const val REFRESH_MS = 15L
    }

    init {
        settings.settings
            .onEach { enabled = it.haptics }
            .launchIn(CoroutineScope(SupervisorJob() + Dispatchers.Default))
    }

    /** Faint tick as the compass needle passes a cardinal point. */
    fun cardinalTick() {
        if (enabled && works) vibrator?.vibrate(VibrationEffect.createOneShot(TICK_MS, VibrationEffect.DEFAULT_AMPLITUDE))
    }

    /** Longer pulse when content is revealed (e.g. fishing card first appears). */
    fun reveal() {
        if (enabled && works) vibrator?.vibrate(VibrationEffect.createOneShot(REVEAL_MS, VibrationEffect.DEFAULT_AMPLITUDE))
    }

    /** Distinct double pulse for a new warning. */
    fun warning() {
        if (enabled && works) vibrator?.vibrate(VibrationEffect.createWaveform(WARNING_PATTERN, -1))
    }

    /** Short blip for a refresh completing. */
    fun refresh() {
        if (enabled && works) vibrator?.vibrate(VibrationEffect.createOneShot(REFRESH_MS, VibrationEffect.DEFAULT_AMPLITUDE))
    }
}