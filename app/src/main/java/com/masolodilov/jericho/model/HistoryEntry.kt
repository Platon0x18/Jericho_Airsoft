package com.masolodilov.jericho.model

sealed interface HistoryEntry {
    val id: String
    val happenedAtMillis: Long

    data class Status(
        val item: StatusHistoryItem,
    ) : HistoryEntry {
        override val id: String = "status_${item.id}"
        override val happenedAtMillis: Long = item.finishedAtMillis
    }

    data class Inventory(
        val item: InventoryLogItem,
    ) : HistoryEntry {
        override val id: String = "inventory_${item.id}"
        override val happenedAtMillis: Long = item.happenedAtMillis
    }
}
