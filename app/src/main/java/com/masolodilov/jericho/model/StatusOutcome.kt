package com.masolodilov.jericho.model

enum class StatusOutcome(val title: String) {
    STARTED("Получен"),
    EXPIRED("Истёк по таймеру"),
    FINISHED_MANUAL("Завершён вручную"),
    CURED("Вылечен"),
    AUTO_TRANSITION("Автопереход"),
    REMOVED_ON_DEATH("Снят из-за смерти"),
    ;

    companion object {
        fun fromName(value: String): StatusOutcome {
            return entries.firstOrNull { it.name == value } ?: FINISHED_MANUAL
        }
    }
}
