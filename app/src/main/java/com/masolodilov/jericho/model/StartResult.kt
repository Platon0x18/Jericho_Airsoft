package com.masolodilov.jericho.model

sealed class StartResult {
    data object Started : StartResult()
    data class StartedAndCured(
        val statusTitle: String,
        val itemTitle: String,
    ) : StartResult()
    data class Blocked(
        val badge: String,
        val reason: String,
    ) : StartResult()
}
