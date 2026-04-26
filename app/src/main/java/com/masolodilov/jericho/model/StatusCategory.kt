package com.masolodilov.jericho.model

enum class StatusCategory(val title: String) {
    DISEASE("Болезни"),
    WOUND("Ранения и состояния"),
    POSITIVE("Положительные эффекты"),
    CONTROL("Контроль и административные"),
    CUSTOM("Свой таймер"),
    ;

    companion object {
        fun fromName(value: String): StatusCategory {
            return entries.firstOrNull { it.name == value } ?: CUSTOM
        }

        fun fromTitle(value: String): StatusCategory {
            return entries.firstOrNull { it.title == value } ?: CUSTOM
        }
    }
}
