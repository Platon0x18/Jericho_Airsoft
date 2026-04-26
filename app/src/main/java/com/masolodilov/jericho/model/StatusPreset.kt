package com.masolodilov.jericho.model

data class StatusPreset(
    val id: String,
    val title: String,
    val category: StatusCategory,
    val durationMinutes: Long,
    val description: String,
    val blockedBy: Set<EffectTag> = emptySet(),
    val grantedEffects: Set<EffectTag> = emptySet(),
    val nextPresetIdOnExpire: String? = null,
)
