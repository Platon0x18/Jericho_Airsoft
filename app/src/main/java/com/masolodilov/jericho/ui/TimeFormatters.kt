package com.masolodilov.jericho.ui

import java.text.DateFormat
import java.util.Date
import java.util.Locale

object TimeFormatters {
    fun durationLabel(minutes: Long): String {
        return if (minutes >= 60) {
            val hours = minutes / 60
            val restMinutes = minutes % 60
            if (restMinutes == 0L) {
                "$hours ч"
            } else {
                "$hours ч $restMinutes мин"
            }
        } else {
            "$minutes мин"
        }
    }

    fun countdown(millis: Long): String {
        val safe = millis.coerceAtLeast(0L)
        val totalSeconds = ((safe + 999L) / 1000L).toInt()
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        return if (hours > 0) {
            String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
        }
    }

    fun dateTime(millis: Long): String {
        return DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(millis))
    }
}

