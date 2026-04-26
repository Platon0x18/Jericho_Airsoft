package com.masolodilov.jericho.model

import java.util.UUID

data class InventoryItem(
    val id: String,
    val title: String,
    val category: InventoryCategory,
    val quantity: Int,
) {
    companion object {
        fun create(
            title: String,
            category: InventoryCategory,
            quantity: Int,
        ): InventoryItem {
            return InventoryItem(
                id = UUID.randomUUID().toString(),
                title = title,
                category = category,
                quantity = quantity,
            )
        }
    }
}
