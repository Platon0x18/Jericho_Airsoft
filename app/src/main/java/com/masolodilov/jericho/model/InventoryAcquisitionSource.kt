package com.masolodilov.jericho.model

enum class InventoryAcquisitionSource(
    val title: String,
    val detailsHint: String? = null,
) {
    OPEN_WORLD(
        title = "Найден в открытом мире",
    ),
    COMBAT(
        title = "Добыт в бою",
        detailsHint = "Позывной, у кого затрофеен предмет",
    ),
    ALLY(
        title = "Получен от соратника",
        detailsHint = "Позывной соратника",
    ),
    BARTER(
        title = "Получен по бартеру",
        detailsHint = "Позывной, у кого куплен или обменян предмет",
    ),
    ;

    val requiresDetails: Boolean
        get() = !detailsHint.isNullOrBlank()

    fun buildLogNote(details: String?): String {
        val normalizedDetails = details?.trim().orEmpty()
        return when (this) {
            OPEN_WORLD -> "Источник: найден в открытом мире."
            COMBAT -> "Источник: добыт в бою. У кого затрофеен: «$normalizedDetails»."
            ALLY -> "Источник: получен от соратника. От кого получен: «$normalizedDetails»."
            BARTER -> "Источник: получен по бартеру. У кого куплен или обменян: «$normalizedDetails»."
        }
    }

    companion object {
        fun fromTitle(value: String): InventoryAcquisitionSource {
            return entries.firstOrNull { it.title == value } ?: OPEN_WORLD
        }
    }
}
