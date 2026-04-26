package com.masolodilov.jericho.data

import android.os.Handler
import android.os.Looper
import com.masolodilov.jericho.model.StatusHistoryItem

class StatusTicker(
    private val repository: StatusRepository,
    private val onExpiredStatuses: (List<StatusHistoryItem>) -> Unit,
) {
    private val handler = Handler(Looper.getMainLooper())
    private var started = false

    private val tickRunnable = object : Runnable {
        override fun run() {
            val expired = repository.tick()
            if (expired.isNotEmpty()) {
                onExpiredStatuses(expired)
            }
            handler.postDelayed(this, TICK_MS)
        }
    }

    fun start() {
        if (started) return
        started = true
        handler.post(tickRunnable)
    }

    fun stop() {
        started = false
        handler.removeCallbacks(tickRunnable)
    }

    private companion object {
        const val TICK_MS = 1000L
    }
}
