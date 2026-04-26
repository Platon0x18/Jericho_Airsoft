package com.masolodilov.jericho.data

import com.masolodilov.jericho.model.StatusCategory
import com.masolodilov.jericho.model.StatusPreset
import com.masolodilov.jericho.model.EffectTag
import com.masolodilov.jericho.model.PermanentEffect

sealed class PresetRow {
    data class Header(val title: String) : PresetRow()
    data class Item(
        val preset: StatusPreset,
        val blockedBadge: String? = null,
        val blockedReason: String? = null,
    ) : PresetRow()
}

object PresetCatalog {
    val presets: List<StatusPreset> = listOf(
        StatusPreset(
            id = "gray_rot",
            title = "Серая гниль",
            category = StatusCategory.DISEASE,
            durationMinutes = 12 * 60L,
            description = "Базовая инфекция. Без лечения смертельна через 12 часов.",
            blockedBy = setOf(EffectTag.IMMUNE_GRAY_ROT, EffectTag.IMMUNE_ALL_DISEASES),
        ),
        StatusPreset(
            id = "death_dance",
            title = "Пляска смерти",
            category = StatusCategory.DISEASE,
            durationMinutes = 6 * 60L,
            description = "Неврологическая болезнь. Без лечения смертельна через 6 часов.",
            blockedBy = setOf(EffectTag.IMMUNE_DEATH_DANCE, EffectTag.IMMUNE_ALL_DISEASES),
        ),
        StatusPreset(
            id = "morok",
            title = "Морок",
            category = StatusCategory.DISEASE,
            durationMinutes = 3 * 60L,
            description = "Самая быстрая болезнь из базовых. Без лечения смертельна через 3 часа.",
            blockedBy = setOf(EffectTag.IMMUNE_MOROK, EffectTag.IMMUNE_ALL_DISEASES),
        ),
        StatusPreset(
            id = "light_wound",
            title = "Лёгкое ранение",
            category = StatusCategory.WOUND,
            durationMinutes = 30,
            description = "До ухудшения состояния или лечения.",
            nextPresetIdOnExpire = "heavy_wound",
        ),
        StatusPreset(
            id = "heavy_wound",
            title = "Тяжёлое ранение",
            category = StatusCategory.WOUND,
            durationMinutes = 10,
            description = "До перехода в критическое состояние.",
            nextPresetIdOnExpire = "critical_state",
        ),
        StatusPreset(
            id = "critical_state",
            title = "Критическое состояние",
            category = StatusCategory.WOUND,
            durationMinutes = 5,
            description = "Последний этап до смерти.",
            nextPresetIdOnExpire = "death_body",
        ),
        StatusPreset(
            id = "concussion",
            title = "Контузия",
            category = StatusCategory.WOUND,
            durationMinutes = 5,
            description = "Потеря сознания по правилам игры.",
            blockedBy = setOf(EffectTag.PROTECTS_FROM_CONCUSSION),
        ),
        StatusPreset(
            id = "death_body",
            title = "Смерть: ожидание на месте",
            category = StatusCategory.WOUND,
            durationMinutes = 5,
            description = "Пять минут на месте смерти до выхода в мертвяк.",
            nextPresetIdOnExpire = "dead_time",
        ),
        StatusPreset(
            id = "captivity",
            title = "Плен",
            category = StatusCategory.CONTROL,
            durationMinutes = 60,
            description = "Максимальная длительность плена по правилам.",
        ),
        StatusPreset(
            id = "dead_time",
            title = "Мертвяк",
            category = StatusCategory.CONTROL,
            durationMinutes = 120,
            description = "Стандартный двухчасовой мертвяк.",
        ),
        StatusPreset(
            id = "execution",
            title = "Казнь",
            category = StatusCategory.CONTROL,
            durationMinutes = 60,
            description = "Минимальная длительность процедуры казни.",
        ),
        StatusPreset(
            id = "immune_gray_rot_temp",
            title = "Иммунитет: Серая гниль",
            category = StatusCategory.POSITIVE,
            durationMinutes = 30,
            description = "Временная защита от запуска таймера Серой гнили.",
            grantedEffects = setOf(EffectTag.IMMUNE_GRAY_ROT),
        ),
        StatusPreset(
            id = "immune_death_dance_temp",
            title = "Иммунитет: Пляска смерти",
            category = StatusCategory.POSITIVE,
            durationMinutes = 30,
            description = "Временная защита от запуска таймера Пляски смерти.",
            grantedEffects = setOf(EffectTag.IMMUNE_DEATH_DANCE),
        ),
        StatusPreset(
            id = "immune_morok_temp",
            title = "Иммунитет: Морок",
            category = StatusCategory.POSITIVE,
            durationMinutes = 30,
            description = "Временная защита от запуска таймера Морока.",
            grantedEffects = setOf(EffectTag.IMMUNE_MOROK),
        ),
        StatusPreset(
            id = "immune_all_diseases_temp",
            title = "Иммунитет: все болезни",
            category = StatusCategory.POSITIVE,
            durationMinutes = 60,
            description = "Полностью блокирует запуск любых таймеров болезней, пока активен.",
            grantedEffects = setOf(EffectTag.IMMUNE_ALL_DISEASES),
        ),
        StatusPreset(
            id = "helmet_or_head_aug",
            title = "Защита от контузии",
            category = StatusCategory.POSITIVE,
            durationMinutes = 60,
            description = "Временная защита от запуска таймера контузии.",
            grantedEffects = setOf(EffectTag.PROTECTS_FROM_CONCUSSION),
        ),
        StatusPreset(
            id = "memory_protection",
            title = "Сохранение памяти",
            category = StatusCategory.POSITIVE,
            durationMinutes = 60,
            description = "Таймер положительного эффекта для способностей, сохраняющих память.",
            grantedEffects = setOf(EffectTag.MEMORY_PROTECTION),
        ),
    )

    val permanentEffects: List<PermanentEffect> = listOf(
        PermanentEffect(
            id = "perm_immune_gray_rot",
            title = "Постоянный иммунитет: Серая гниль",
            description = "Блокирует запуск таймера Серой гнили.",
            grantedEffects = setOf(EffectTag.IMMUNE_GRAY_ROT),
        ),
        PermanentEffect(
            id = "perm_immune_death_dance",
            title = "Постоянный иммунитет: Пляска смерти",
            description = "Блокирует запуск таймера Пляски смерти.",
            grantedEffects = setOf(EffectTag.IMMUNE_DEATH_DANCE),
        ),
        PermanentEffect(
            id = "perm_immune_morok",
            title = "Постоянный иммунитет: Морок",
            description = "Блокирует запуск таймера Морока.",
            grantedEffects = setOf(EffectTag.IMMUNE_MOROK),
        ),
        PermanentEffect(
            id = "perm_immune_all",
            title = "Постоянный иммунитет: все болезни",
            description = "Полностью блокирует запуск любых болезней.",
            grantedEffects = setOf(EffectTag.IMMUNE_ALL_DISEASES),
        ),
        PermanentEffect(
            id = "perm_concussion_protection",
            title = "Постоянная защита от контузии",
            description = "Например, шлем или постоянная защита головы.",
            grantedEffects = setOf(EffectTag.PROTECTS_FROM_CONCUSSION),
        ),
        PermanentEffect(
            id = "perm_memory_protection",
            title = "Постоянное сохранение памяти",
            description = "Для эффектов, отменяющих потерю памяти после смерти или контузии.",
            grantedEffects = setOf(EffectTag.MEMORY_PROTECTION),
        ),
    )

    fun presetById(id: String): StatusPreset? = presets.firstOrNull { it.id == id }

    fun permanentEffectById(id: String): PermanentEffect? = permanentEffects.firstOrNull { it.id == id }

    fun rows(): List<PresetRow> {
        return buildList {
            StatusCategory.entries
                .filter { it != StatusCategory.CUSTOM }
                .forEach { category ->
                    add(PresetRow.Header(category.title))
                    presets.filter { it.category == category }.forEach { preset ->
                        add(PresetRow.Item(preset))
                    }
                }
        }
    }
}
