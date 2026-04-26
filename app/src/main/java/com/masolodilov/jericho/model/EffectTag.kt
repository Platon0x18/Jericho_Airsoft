package com.masolodilov.jericho.model

enum class EffectTag(val title: String) {
    IMMUNE_GRAY_ROT("Иммунитет к Серой гнили"),
    IMMUNE_DEATH_DANCE("Иммунитет к Пляске смерти"),
    IMMUNE_MOROK("Иммунитет к Мороку"),
    IMMUNE_ALL_DISEASES("Иммунитет ко всем болезням"),
    PROTECTS_FROM_CONCUSSION("Защита от контузии"),
    MEMORY_PROTECTION("Сохранение памяти"),
    ;

    companion object {
        fun fromName(value: String): EffectTag? {
            return entries.firstOrNull { it.name == value }
        }

        fun decodeList(raw: String?): Set<EffectTag> {
            if (raw.isNullOrBlank()) return emptySet()
            return raw.split(",")
                .mapNotNull { fromName(it.trim()) }
                .toSet()
        }

        fun encodeList(values: Set<EffectTag>): String {
            return values.joinToString(",") { it.name }
        }
    }
}

