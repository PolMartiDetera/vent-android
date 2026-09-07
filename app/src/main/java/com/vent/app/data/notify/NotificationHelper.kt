package com.vent.app.data.notify

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.vent.app.MainActivity
import com.vent.app.R
import com.vent.app.domain.alerts.AlertSeverity
import com.vent.app.domain.alerts.ThresholdAlertResult

/** Posts the daily brief and threshold/serious alerts through distinct channels. */
class NotificationHelper(private val context: Context) {

    fun postDailyBrief(text: String) {
        val notification = baseBuilder(NotificationChannels.DAILY_BRIEF, R.drawable.ic_launcher_foreground)
            .setContentTitle("Vent · Daily brief")
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .build()
        notify(1001, notification)
    }

    fun postThreshold(id: Int, result: ThresholdAlertResult) {
        val channel = if (result.severity == AlertSeverity.SEVERE) {
            NotificationChannels.SEVERE
        } else {
            NotificationChannels.THRESHOLD
        }
        val notification = baseBuilder(channel, R.drawable.ic_launcher_foreground)
            .setContentTitle(if (result.severity == AlertSeverity.SEVERE) "Vent · Severe weather" else "Vent · Threshold alert")
            .setContentText(result.message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(result.message))
            .setAutoCancel(true)
            .build()
        notify(id, notification)
    }

    private fun baseBuilder(channel: String, smallIcon: Int): NotificationCompat.Builder =
        NotificationCompat.Builder(context, channel)
            .setSmallIcon(smallIcon)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(launchPendingIntent())

    private fun launchPendingIntent(): PendingIntent {
        val intent = Intent(context, MainActivity::class.java)
        return PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun notify(id: Int, notification: android.app.Notification) {
        runCatching {
            NotificationManagerCompat.from(context).notify(id, notification)
        }
    }
}