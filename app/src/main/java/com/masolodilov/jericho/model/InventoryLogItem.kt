package com.masolodilov.jericho.model

import java.util.UUID

data class InventoryLogItem(
    val id: String,
    val title: String,
    val category: InventoryCategory,
    val action: InventoryAction,
    val quantity: Int,
    val totalAfter: Int,
    val happenedAtMillis: Long,
    val note: String? = null,
) {
    companion object {
        fun create(
            title: String,
            category: InventoryCategory,
            action: InventoryAction,
            quantity: Int,
            totalAfter: Int,
            happenedAtMillis: Long = System.currentTimeMillis(),
            note: String? = null,
        ): InventoryLogItem {
            return InventoryLogItem(
                id = UUID.randomUUID().toString(),
                title = title,
                category = category,
                action = action,
                quantity = quantity,
                totalAfter = totalAfter,
                happenedAtMillis = happenedAtMillis,
                note = note,
            )
        }
    }
}
