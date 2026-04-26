package com.masolodilov.jericho.model

enum class InventoryAction(val title: String) {
    ACQUIRED("Получен"),
    SPENT("Потрачен"),
    TRANSFERRED("Передан"),
    RECEIVED_QR("Получен по QR"),
    ;

    companion object {
        fun fromName(value: String): InventoryAction {
            return entries.firstOrNull { it.name == value } ?: ACQUIRED
        }
    }
}
