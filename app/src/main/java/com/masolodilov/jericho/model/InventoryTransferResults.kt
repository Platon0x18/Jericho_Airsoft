package com.masolodilov.jericho.model

sealed interface InventoryTransferCreateResult {
    data class Success(
        val payload: InventoryTransferPayload,
        val qrContent: String,
    ) : InventoryTransferCreateResult

    data class Error(val message: String) : InventoryTransferCreateResult
}

sealed interface InventoryTransferReceiveResult {
    data class Success(
        val payload: InventoryTransferPayload,
        val totalAfter: Int,
    ) : InventoryTransferReceiveResult

    data class Error(val message: String) : InventoryTransferReceiveResult
}
