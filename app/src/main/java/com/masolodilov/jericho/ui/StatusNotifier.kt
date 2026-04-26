package com.masolodilov.jericho.ui

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.masolodilov.jericho.R
import com.masolodilov.jericho.model.StatusHistoryItem

class StatusNotifier(
    private val context: Context,
) {
    fun ensureChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.notification_channel_name),
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = context.getString(R.string.notification_channel_description)
        }

        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    fun notifyExpiredStatuses(items: List<StatusHistoryItem>) {
        if (!canPostNotifications()) return

        val notificationManager = NotificationManagerCompat.from(context)
        items.forEach { item ->
            notificationManager.notify(item.id.hashCode(), buildExpiredNotification(item))
        }
    }

    private fun buildExpiredNotification(item: StatusHistoryItem) =
        NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle(notificationTitle(item))
            .setContentText(notificationText(item))
            .setStyle(
                NotificationCompat.BigTextStyle().bigText(
                    notificationText(item),
                ),
            )
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(contentIntent())
            .build()

    private fun notificationTitle(item: StatusHistoryItem): String {
        return when {
            item.outcome == com.masolodilov.jericho.model.StatusOutcome.AUTO_TRANSITION && !item.note.isNullOrBlank() -> {
                context.getString(R.string.notification_transition_title)
            }
            else -> context.getString(R.string.notification_expired_title)
        }
    }

    private fun notificationText(item: StatusHistoryItem): String {
        return when {
            item.outcome == com.masolodilov.jericho.model.StatusOutcome.AUTO_TRANSITION && !item.note.isNullOrBlank() -> {
                context.getString(R.string.notification_transition_text, item.title, item.note)
            }
            else -> context.getString(R.string.notification_expired_text, item.title)
        }
    }

    private fun contentIntent(): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return PendingIntent.getActivity(
            context,
            1001,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun canPostNotifications(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    private companion object {
        const val CHANNEL_ID = "jericho_status_expired"
    }
}
