package com.masolodilov.jericho.model

import java.util.UUID

data class TrackedStatus(
    val id: String,
    val presetId: String? = null,
    val title: String,
    val category: StatusCategory,
    val durationMillis: Long,
    val startedAtMillis: Long,
    val pausedAtMillis: Long? = null,
    val totalPausedMillis: Long = 0L,
    val description: String = "",
    val grantedEffects: Set<EffectTag> = emptySet(),
) {
    fun elapsedAt(now: Long): Long {
        val current = pausedAtMillis ?: now
        return (current - startedAtMillis - totalPausedMillis).coerceAtLeast(0L)
    }

    fun remainingAt(now: Long): Long {
        return (durationMillis - elapsedAt(now)).coerceAtLeast(0L)
    }

    fun progressPercentAt(now: Long): Int {
        if (durationMillis <= 0L) return 100
        val elapsed = elapsedAt(now).coerceAtMost(durationMillis)
        return ((elapsed * 100L) / durationMillis).toInt()
    }

    fun isPaused(): Boolean = pausedAtMillis != null

    fun pause(now: Long): TrackedStatus {
        return if (pausedAtMillis == null) copy(pausedAtMillis = now) else this
    }

    fun resume(now: Long): TrackedStatus {
        val pausedAt = pausedAtMillis ?: return this
        return copy(
            pausedAtMillis = null,
            totalPausedMillis = totalPausedMillis + (now - pausedAt),
        )
    }

    companion object {
        fun fromPreset(preset: StatusPreset, now: Long = System.currentTimeMillis()): TrackedStatus {
            return TrackedStatus(
                id = UUID.randomUUID().toString(),
                presetId = preset.id,
                title = preset.title,
                category = preset.category,
                durationMillis = preset.durationMinutes * 60_000L,
                startedAtMillis = now,
                description = preset.description,
                grantedEffects = preset.grantedEffects,
            )
        }

        fun createCustom(
            title: String,
            category: StatusCategory,
            durationMinutes: Long,
            now: Long = System.currentTimeMillis(),
        ): TrackedStatus {
            return TrackedStatus(
                id = UUID.randomUUID().toString(),
                presetId = null,
                title = title,
                category = category,
                durationMillis = durationMinutes * 60_000L,
                startedAtMillis = now,
                description = "Пользовательский таймер",
                grantedEffects = emptySet(),
            )
        }
    }
}
