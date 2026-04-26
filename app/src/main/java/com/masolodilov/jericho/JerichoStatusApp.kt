package com.masolodilov.jericho

import android.app.Application
import com.masolodilov.jericho.data.StatusRepository
import com.masolodilov.jericho.data.StatusTicker
import com.masolodilov.jericho.ui.StatusNotifier
import com.masolodilov.jericho.ui.TrackerProvider

class JerichoStatusApp : Application(), TrackerProvider {
    override val repository: StatusRepository by lazy { StatusRepository(applicationContext) }

    private val notifier: StatusNotifier by lazy { StatusNotifier(applicationContext) }
    private val ticker: StatusTicker by lazy {
        StatusTicker(repository) { expiredStatuses ->
            notifier.notifyExpiredStatuses(expiredStatuses)
        }
    }

    override fun onCreate() {
        super.onCreate()
        notifier.ensureChannel()
        ticker.start()
    }
}

