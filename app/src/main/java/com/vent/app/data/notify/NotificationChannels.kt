package com.vent.app.data.notify

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build

object NotificationChannels {
    const val DAILY_BRIEF = "daily_brief"
    const val THRESHOLD = "threshold"
    const val SEVERE = "severe"

    fun createChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        val channels = listOf(
            NotificationChannel(
                DAILY_BRIEF,
                "Daily brief",
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply { description = "Morning sailing and fishing summary" },
            NotificationChannel(
                THRESHOLD,
                "Threshold alerts",
                NotificationManager.IMPORTANCE_HIGH,
            ).apply { description = "Your custom wind, gust, and wave thresholds" },
            NotificationChannel(
                SEVERE,
                "Severe weather",
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = "Rapid escalations beyond your thresholds"
                enableVibration(true)
            },
        )
        manager.createNotificationChannels(channels)
    }
}