package com.masolodilov.jericho.ui

import com.masolodilov.jericho.data.StatusRepository

interface TrackerProvider {
    val repository: StatusRepository
}
