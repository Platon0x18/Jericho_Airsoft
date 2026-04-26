package com.masolodilov.jericho.model

data class InventoryTransferPayload(
    val transferId: String,
    val title: String,
    val category: InventoryCategory,
    val quantity: Int,
    val createdAtMillis: Long,
)
