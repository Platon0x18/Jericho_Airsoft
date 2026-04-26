package com.masolodilov.jericho.model

data class StatusHistoryItem(
    val id: String,
    val title: String,
    val category: StatusCategory,
    val durationMillis: Long,
    val finishedAtMillis: Long,
    val outcome: StatusOutcome,
    val note: String? = null,
)
