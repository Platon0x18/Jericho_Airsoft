package com.masolodilov.jericho.model

data class PermanentEffect(
    val id: String,
    val title: String,
    val description: String,
    val grantedEffects: Set<EffectTag>,
)

