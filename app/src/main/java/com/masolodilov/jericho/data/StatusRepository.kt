package com.masolodilov.jericho.data

import android.content.Context
import com.masolodilov.jericho.model.EffectTag
import com.masolodilov.jericho.model.CureResult
import com.masolodilov.jericho.model.HistoryEntry
import com.masolodilov.jericho.model.InventoryAction
import com.masolodilov.jericho.model.InventoryCategory
import com.masolodilov.jericho.model.InventoryItem
import com.masolodilov.jericho.model.InventoryLogItem
import com.masolodilov.jericho.model.InventoryTransferCreateResult
import com.masolodilov.jericho.model.InventoryTransferPayload
import com.masolodilov.jericho.model.InventoryTransferReceiveResult
import com.masolodilov.jericho.model.PermanentEffectState
import com.masolodilov.jericho.model.PlayerProfile
import com.masolodilov.jericho.model.StartResult
import com.masolodilov.jericho.model.StatusCategory
import com.masolodilov.jericho.model.StatusCureOption
import com.masolodilov.jericho.model.StatusHistoryItem
import com.masolodilov.jericho.model.StatusOutcome
import com.masolodilov.jericho.model.StatusPreset
import com.masolodilov.jericho.model.TrackedStatus
import java.util.UUID

class StatusRepository(context: Context) {
    interface Listener {
        fun onStatusStateChanged()
    }

    private companion object {
        const val DEAD_TIME_PRESET_ID = "dead_time"
    }

    private val storage = StatusStorage(context)
    private val listeners = linkedSetOf<Listener>()
    private val activeStatuses = storage.loadActiveStatuses().toMutableList()
    private val historyItems = storage.loadHistoryItems().toMutableList()
    private val enabledPermanentEffectIds = storage.loadEnabledPermanentEffectIds().toMutableSet()
    private val inventoryItems = storage.loadInventoryItems().toMutableList()
    private val inventoryLogItems = storage.loadInventoryLogItems().toMutableList()
    private val receivedTransferIds = storage.loadReceivedTransferIds().toMutableSet()
    private var playerProfile = storage.loadPlayerProfile()

    fun presetRows(): List<PresetRow> {
        return PresetCatalog.rows().map { row ->
            when (row) {
                is PresetRow.Header -> row
                is PresetRow.Item -> {
                    val blocked = blockedStartFor(row.preset)
                    row.copy(
                        blockedBadge = blocked?.badge,
                        blockedReason = blocked?.reason,
                    )
                }
            }
        }
    }

    fun categoryTitles(): List<String> = StatusCategory.entries.map { it.title }

    fun getPermanentEffects(): List<PermanentEffectState> {
        return PresetCatalog.permanentEffects.map { effect ->
            PermanentEffectState(
                effect = effect,
                enabled = effect.id in enabledPermanentEffectIds,
            )
        }
    }

    fun getActiveStatuses(now: Long = System.currentTimeMillis()): List<TrackedStatus> {
        return activeStatuses.sortedBy { it.remainingAt(now) }
    }

    fun getHistoryEntries(): List<HistoryEntry> {
        return buildList {
            historyItems.forEach { add(HistoryEntry.Status(it)) }
            inventoryLogItems.forEach { add(HistoryEntry.Inventory(it)) }
        }.sortedByDescending { it.happenedAtMillis }
    }

    fun getInventoryItems(): List<InventoryItem> {
        return inventoryItems.sortedWith(
            compareBy<InventoryItem> { it.category.ordinal }.thenBy { it.title.lowercase() },
        )
    }

    fun getPlayerProfile(): PlayerProfile = playerProfile

    fun createInventoryTransfer(itemId: String, quantity: Int): InventoryTransferCreateResult {
        if (quantity <= 0) {
            return InventoryTransferCreateResult.Error("Укажите количество больше нуля.")
        }

        val item = inventoryItems.firstOrNull { it.id == itemId }
            ?: return InventoryTransferCreateResult.Error("Предмет уже недоступен.")
        if (item.quantity < quantity) {
            return InventoryTransferCreateResult.Error("Нельзя передать больше, чем есть в инвентаре.")
        }

        val now = System.currentTimeMillis()
        val payload = InventoryTransferPayload(
            transferId = UUID.randomUUID().toString(),
            title = item.title,
            category = item.category,
            quantity = quantity,
            createdAtMillis = now,
        )

        spendInventoryAmountInternal(
            itemId = item.id,
            quantity = quantity,
            happenedAtMillis = now,
            action = InventoryAction.TRANSFERRED,
        ) ?: return InventoryTransferCreateResult.Error("Не удалось списать предметы для передачи.")

        persistAndNotify()
        return InventoryTransferCreateResult.Success(
            payload = payload,
            qrContent = InventoryTransferCodec.encode(payload),
        )
    }

    fun receiveInventoryTransfer(rawValue: String): InventoryTransferReceiveResult {
        val payload = InventoryTransferCodec.decode(rawValue)
            ?: return InventoryTransferReceiveResult.Error("Этот QR-код не похож на передачу инвентаря.")

        if (payload.transferId in receivedTransferIds) {
            return InventoryTransferReceiveResult.Error("Этот QR-код уже был принят на этом устройстве.")
        }

        val existingIndex = inventoryItems.indexOfFirst {
            it.category == payload.category && it.title.equals(payload.title, ignoreCase = true)
        }

        val updatedItem = if (existingIndex >= 0) {
            val existing = inventoryItems[existingIndex]
            val updated = existing.copy(quantity = existing.quantity + payload.quantity)
            inventoryItems[existingIndex] = updated
            updated
        } else {
            val created = InventoryItem.create(
                title = payload.title,
                category = payload.category,
                quantity = payload.quantity,
            )
            inventoryItems += created
            created
        }

        inventoryLogItems += InventoryLogItem.create(
            title = updatedItem.title,
            category = updatedItem.category,
            action = InventoryAction.RECEIVED_QR,
            quantity = payload.quantity,
            totalAfter = updatedItem.quantity,
        )
        receivedTransferIds += payload.transferId
        persistAndNotify()
        return InventoryTransferReceiveResult.Success(
            payload = payload,
            totalAfter = updatedItem.quantity,
        )
    }

    fun hasCureSupport(status: TrackedStatus): Boolean = cureCategoriesFor(status).isNotEmpty()

    fun getCureOption(status: TrackedStatus): StatusCureOption? {
        val cureCategories = cureCategoriesFor(status)
        if (cureCategories.isEmpty()) return null

        return cureCategories.firstNotNullOfOrNull { category ->
            inventoryItems.firstOrNull { it.category == category && it.quantity > 0 }?.let { item ->
                StatusCureOption(
                    inventoryItemId = item.id,
                    itemTitle = item.title,
                    itemCategory = item.category,
                )
            }
        }
    }

    fun getImmediateCureOption(preset: StatusPreset): StatusCureOption? {
        val cureCategories = cureCategoriesFor(preset.id)
        if (cureCategories.isEmpty()) return null

        return cureCategories.firstNotNullOfOrNull { category ->
            inventoryItems.firstOrNull { it.category == category && it.quantity > 0 }?.let { item ->
                StatusCureOption(
                    inventoryItemId = item.id,
                    itemTitle = item.title,
                    itemCategory = item.category,
                )
            }
        }
    }

    fun addListener(listener: Listener) {
        listeners += listener
        listener.onStatusStateChanged()
    }

    fun removeListener(listener: Listener) {
        listeners -= listener
    }

    fun startPreset(preset: StatusPreset): StartResult {
        blockedStartFor(preset)?.let { return it }
        val now = System.currentTimeMillis()
        spendPositiveActivationItemIfNeeded(
            preset = preset,
            happenedAtMillis = now,
        )?.let { return it }
        startTrackedStatus(TrackedStatus.fromPreset(preset, now))
        persistAndNotify()
        return StartResult.Started
    }

    fun startPresetWithImmediateCure(preset: StatusPreset): StartResult {
        blockedStartFor(preset)?.let { return it }

        val now = System.currentTimeMillis()
        val status = TrackedStatus.fromPreset(preset, now)
        val cureOption = getImmediateCureOption(preset)

        startTrackedStatus(
            status,
            shouldPersist = false,
            shouldNotify = false,
        )

        if (cureOption == null) {
            persistAndNotify()
            return StartResult.Started
        }

        val spent = spendInventoryAmountInternal(
            itemId = cureOption.inventoryItemId,
            quantity = 1,
            happenedAtMillis = now,
            logSpent = false,
        )

        if (spent == null) {
            persistAndNotify()
            return StartResult.Started
        }

        val cured = finishInternal(
            statusId = status.id,
            outcome = StatusOutcome.CURED,
            finishedAtMillis = now,
            note = "Вылечен предметом: ${spent.title}",
            shouldPersist = false,
            shouldNotify = false,
        )

        if (cured == null) {
            persistAndNotify()
            return StartResult.Started
        }

        persistAndNotify()
        return StartResult.StartedAndCured(
            statusTitle = status.title,
            itemTitle = spent.title,
        )
    }

    fun startCustom(title: String, durationMinutes: Long, category: StatusCategory): StartResult {
        deadTimeLockFor(title)?.let { return it }
        duplicateStartFor(
            presetId = null,
            title = title,
            category = category,
        )?.let { return it }
        startTrackedStatus(
            TrackedStatus.createCustom(
                title = title,
                category = category,
                durationMinutes = durationMinutes,
            ),
        )
        persistAndNotify()
        return StartResult.Started
    }

    fun setPermanentEffectEnabled(effectId: String, enabled: Boolean) {
        val changed = if (enabled) {
            enabledPermanentEffectIds.add(effectId)
        } else {
            enabledPermanentEffectIds.remove(effectId)
        }
        if (!changed) return
        persistAndNotify()
    }

    fun pauseOrResume(statusId: String) {
        val now = System.currentTimeMillis()
        val index = activeStatuses.indexOfFirst { it.id == statusId }
        if (index == -1) return

        val status = activeStatuses[index]
        activeStatuses[index] = if (status.isPaused()) {
            status.resume(now)
        } else {
            status.pause(now)
        }
        persistAndNotify()
    }

    fun finishStatus(statusId: String, outcome: StatusOutcome = StatusOutcome.FINISHED_MANUAL) {
        finishInternal(statusId = statusId, outcome = outcome, finishedAtMillis = System.currentTimeMillis())
    }

    fun clearHistory() {
        historyItems.clear()
        inventoryLogItems.clear()
        persistAndNotify()
    }

    fun addInventoryItem(
        title: String,
        category: InventoryCategory,
        quantity: Int,
        note: String? = null,
    ) {
        val normalizedTitle = title.trim()
        if (normalizedTitle.isBlank() || quantity <= 0) return

        val existingIndex = inventoryItems.indexOfFirst {
            it.category == category && it.title.equals(normalizedTitle, ignoreCase = true)
        }

        val updatedItem = if (existingIndex >= 0) {
            val existing = inventoryItems[existingIndex]
            val updated = existing.copy(quantity = existing.quantity + quantity)
            inventoryItems[existingIndex] = updated
            updated
        } else {
            val created = InventoryItem.create(
                title = normalizedTitle,
                category = category,
                quantity = quantity,
            )
            inventoryItems += created
            created
        }

        inventoryLogItems += InventoryLogItem.create(
            title = updatedItem.title,
            category = updatedItem.category,
            action = InventoryAction.ACQUIRED,
            quantity = quantity,
            totalAfter = updatedItem.quantity,
            note = note,
        )
        persistAndNotify()
    }

    fun addInventoryAmount(itemId: String, quantity: Int): Boolean {
        if (quantity <= 0) return false
        val index = inventoryItems.indexOfFirst { it.id == itemId }
        if (index == -1) return false

        val item = inventoryItems[index]
        val updated = item.copy(quantity = item.quantity + quantity)
        inventoryItems[index] = updated
        inventoryLogItems += InventoryLogItem.create(
            title = updated.title,
            category = updated.category,
            action = InventoryAction.ACQUIRED,
            quantity = quantity,
            totalAfter = updated.quantity,
        )
        persistAndNotify()
        return true
    }

    fun spendInventoryAmount(itemId: String, quantity: Int): Boolean {
        spendInventoryAmountInternal(
            itemId = itemId,
            quantity = quantity,
            happenedAtMillis = System.currentTimeMillis(),
        ) ?: return false
        persistAndNotify()
        return true
    }

    fun savePlayerProfile(profile: PlayerProfile) {
        playerProfile = profile
        persistAndNotify()
    }

    fun cureStatus(statusId: String): CureResult {
        val status = activeStatuses.firstOrNull { it.id == statusId }
            ?: return CureResult.Unavailable("Статус уже недоступен.")
        val cureOption = getCureOption(status)
            ?: return CureResult.Unavailable("Подходящий предмет для лечения не найден.")

        val now = System.currentTimeMillis()
        val spent = spendInventoryAmountInternal(
            itemId = cureOption.inventoryItemId,
            quantity = 1,
            happenedAtMillis = now,
            logSpent = false,
        ) ?: return CureResult.Unavailable("Не удалось использовать предмет для лечения.")

        finishInternal(
            statusId = statusId,
            outcome = StatusOutcome.CURED,
            finishedAtMillis = now,
            note = "Вылечен предметом: ${spent.title}",
            shouldPersist = false,
            shouldNotify = false,
        ) ?: return CureResult.Unavailable("Не удалось закрыть таймер недуга.")

        persistAndNotify()
        return CureResult.Success(
            statusTitle = status.title,
            itemTitle = spent.title,
        )
    }

    fun tick(): List<StatusHistoryItem> {
        val now = System.currentTimeMillis()
        val finishedItems = mutableListOf<StatusHistoryItem>()
        val expired = activeStatuses.filter { !it.isPaused() && it.remainingAt(now) <= 0L }
        if (expired.isNotEmpty()) {
            expired.forEach { status ->
                val nextPreset = status.presetId
                    ?.let(PresetCatalog::presetById)
                    ?.nextPresetIdOnExpire
                    ?.let(PresetCatalog::presetById)

                if (nextPreset != null) {
                    finishInternal(
                        statusId = status.id,
                        outcome = StatusOutcome.AUTO_TRANSITION,
                        finishedAtMillis = now,
                        note = nextPreset.title,
                        shouldPersist = false,
                        shouldNotify = false,
                    )?.let(finishedItems::add)
                    startTrackedStatus(
                        TrackedStatus.fromPreset(nextPreset, now),
                        shouldPersist = false,
                        shouldNotify = false,
                    )
                } else {
                    finishInternal(
                        statusId = status.id,
                        outcome = StatusOutcome.EXPIRED,
                        finishedAtMillis = now,
                        shouldPersist = false,
                        shouldNotify = false,
                    )?.let(finishedItems::add)
                }
            }
            persist()
        }

        if (activeStatuses.isNotEmpty() || expired.isNotEmpty()) {
            notifyListeners()
        }
        return finishedItems
    }

    private fun finishInternal(
        statusId: String,
        outcome: StatusOutcome,
        finishedAtMillis: Long,
        note: String? = null,
        shouldPersist: Boolean = true,
        shouldNotify: Boolean = true,
    ): StatusHistoryItem? {
        val index = activeStatuses.indexOfFirst { it.id == statusId }
        if (index == -1) return null

        val removed = activeStatuses.removeAt(index)
        val historyItem = StatusHistoryItem(
            id = removed.id,
            title = removed.title,
            category = removed.category,
            durationMillis = removed.durationMillis,
            finishedAtMillis = finishedAtMillis,
            outcome = outcome,
            note = note,
        )
        historyItems += historyItem
        if (shouldPersist) {
            persist()
        }
        if (shouldNotify) {
            notifyListeners()
        }
        return historyItem
    }

    private fun persistAndNotify() {
        persist()
        notifyListeners()
    }

    private fun startTrackedStatus(
        status: TrackedStatus,
        shouldPersist: Boolean = false,
        shouldNotify: Boolean = false,
    ) {
        val statusesClearedOnDeath = if (status.presetId == DEAD_TIME_PRESET_ID) {
            activeStatuses.filter { it.category.isClearedOnDeath() }
        } else {
            emptyList()
        }

        activeStatuses += status
        historyItems += StatusHistoryItem(
            id = UUID.randomUUID().toString(),
            title = status.title,
            category = status.category,
            durationMillis = status.durationMillis,
            finishedAtMillis = status.startedAtMillis,
            outcome = StatusOutcome.STARTED,
            note = startHistoryNote(status, statusesClearedOnDeath),
        )

        statusesClearedOnDeath.forEach { clearedStatus ->
            finishInternal(
                statusId = clearedStatus.id,
                outcome = StatusOutcome.REMOVED_ON_DEATH,
                finishedAtMillis = status.startedAtMillis,
                note = "Эффект снят из-за смерти.",
                shouldPersist = false,
                shouldNotify = false,
            )
        }

        if (shouldPersist) {
            persist()
        }
        if (shouldNotify) {
            notifyListeners()
        }
    }

    private fun spendInventoryAmountInternal(
        itemId: String,
        quantity: Int,
        happenedAtMillis: Long,
        logSpent: Boolean = true,
        action: InventoryAction = InventoryAction.SPENT,
    ): InventoryLogItem? {
        if (quantity <= 0) return null
        val index = inventoryItems.indexOfFirst { it.id == itemId }
        if (index == -1) return null

        val item = inventoryItems[index]
        if (item.quantity < quantity) return null

        val remaining = item.quantity - quantity
        if (remaining > 0) {
            inventoryItems[index] = item.copy(quantity = remaining)
        } else {
            inventoryItems.removeAt(index)
        }

        val logItem = InventoryLogItem.create(
            title = item.title,
            category = item.category,
            action = action,
            quantity = quantity,
            totalAfter = remaining,
            happenedAtMillis = happenedAtMillis,
        )
        if (logSpent) {
            inventoryLogItems += logItem
        }
        return logItem
    }

    private fun persist() {
        storage.save(
            activeStatuses = activeStatuses,
            historyItems = historyItems,
            enabledPermanentEffectIds = enabledPermanentEffectIds,
            inventoryItems = inventoryItems,
            inventoryLogItems = inventoryLogItems,
            receivedTransferIds = receivedTransferIds,
            playerProfile = playerProfile,
        )
    }

    private fun notifyListeners() {
        listeners.forEach { it.onStatusStateChanged() }
    }

    private fun blockedStartFor(preset: StatusPreset): StartResult.Blocked? {
        deadTimeLockFor(preset.title)?.let { return it }

        duplicateStartFor(
            presetId = preset.id,
            title = preset.title,
            category = preset.category,
        )?.let { return it }

        positiveActivationBlockedFor(preset)?.let { return it }

        val activeEffects = activeEffectTags()
        val blockingEffect = preset.blockedBy.firstOrNull { it in activeEffects } ?: return null
        val (badge, reason) = when (blockingEffect) {
            EffectTag.IMMUNE_GRAY_ROT -> {
                "Иммунитет" to "активен иммунитет к Серой гнили."
            }
            EffectTag.IMMUNE_DEATH_DANCE -> {
                "Иммунитет" to "активен иммунитет к Пляске смерти."
            }
            EffectTag.IMMUNE_MOROK -> {
                "Иммунитет" to "активен иммунитет к Мороку."
            }
            EffectTag.IMMUNE_ALL_DISEASES -> {
                "Иммунитет" to "активен иммунитет ко всем болезням."
            }
            EffectTag.PROTECTS_FROM_CONCUSSION -> {
                "Защита" to "активна защита от контузии."
            }
            EffectTag.MEMORY_PROTECTION -> {
                "Защита" to "активна защита, блокирующая этот эффект."
            }
        }
        return StartResult.Blocked(
            badge = badge,
            reason = "Таймер «${preset.title}» не запущен: $reason",
        )
    }

    private fun activeEffectTags(): Set<EffectTag> {
        return buildSet {
            activeStatuses.forEach { addAll(it.grantedEffects) }
            enabledPermanentEffectIds.forEach { effectId ->
                PresetCatalog.permanentEffectById(effectId)?.let { addAll(it.grantedEffects) }
            }
        }
    }

    private fun positiveActivationBlockedFor(preset: StatusPreset): StartResult.Blocked? {
        if (preset.category != StatusCategory.POSITIVE) return null
        val requiredCategories = activationCategoriesFor(preset.id)
        if (requiredCategories.isEmpty()) return null
        if (activationItemOptionFor(preset) != null) return null

        return StartResult.Blocked(
            badge = "Нет зелья",
            reason = "Эффект «${preset.title}» не запущен: в инвентаре нет подходящего зелья или лекарства.",
        )
    }

    private fun spendPositiveActivationItemIfNeeded(
        preset: StatusPreset,
        happenedAtMillis: Long,
    ): StartResult.Blocked? {
        if (preset.category != StatusCategory.POSITIVE) return null
        val requiredCategories = activationCategoriesFor(preset.id)
        if (requiredCategories.isEmpty()) return null

        val activationOption = activationItemOptionFor(preset)
            ?: return StartResult.Blocked(
                badge = "Нет зелья",
                reason = "Эффект «${preset.title}» не запущен: в инвентаре нет подходящего зелья или лекарства.",
            )

        val spent = spendInventoryAmountInternal(
            itemId = activationOption.inventoryItemId,
            quantity = 1,
            happenedAtMillis = happenedAtMillis,
        )
        return if (spent == null) {
            StartResult.Blocked(
                badge = "Нет зелья",
                reason = "Эффект «${preset.title}» не запущен: не удалось использовать подходящий предмет.",
            )
        } else {
            null
        }
    }

    private fun activationItemOptionFor(preset: StatusPreset): StatusCureOption? {
        val activationCategories = activationCategoriesFor(preset.id)
        if (activationCategories.isEmpty()) return null

        return activationCategories.firstNotNullOfOrNull { category ->
            inventoryItems.firstOrNull { it.category == category && it.quantity > 0 }?.let { item ->
                StatusCureOption(
                    inventoryItemId = item.id,
                    itemTitle = item.title,
                    itemCategory = item.category,
                )
            }
        }
    }

    private fun cureCategoriesFor(status: TrackedStatus): List<InventoryCategory> {
        return cureCategoriesFor(status.presetId)
    }

    private fun cureCategoriesFor(presetId: String?): List<InventoryCategory> {
        return when (presetId) {
            "gray_rot" -> listOf(InventoryCategory.ANTIBIOTIC, InventoryCategory.GREEN_POTION)
            "death_dance" -> listOf(InventoryCategory.BLUE_POTION)
            "morok" -> listOf(InventoryCategory.PURPLE_POTION)
            else -> emptyList()
        }
    }

    private fun activationCategoriesFor(presetId: String?): List<InventoryCategory> {
        return when (presetId) {
            "immune_gray_rot_temp" -> listOf(InventoryCategory.GREEN_POTION)
            "immune_death_dance_temp" -> listOf(InventoryCategory.BLUE_POTION)
            "immune_morok_temp" -> listOf(InventoryCategory.PURPLE_POTION)
            "immune_all_diseases_temp" -> listOf(InventoryCategory.ANTIBIOTIC)
            else -> emptyList()
        }
    }

    private fun duplicateStartFor(
        presetId: String?,
        title: String,
        category: StatusCategory,
    ): StartResult.Blocked? {
        val hasDuplicate = activeStatuses.any { active ->
            when {
                presetId != null && active.presetId != null -> active.presetId == presetId
                else -> active.title.equals(title, ignoreCase = true) && active.category == category
            }
        }
        if (!hasDuplicate) return null

        return StartResult.Blocked(
            badge = "Активен",
            reason = "Таймер «$title» не запущен: такой же таймер уже активен.",
        )
    }

    private fun deadTimeLockFor(title: String): StartResult.Blocked? {
        val deadTimeActive = activeStatuses.any { it.presetId == DEAD_TIME_PRESET_ID }
        if (!deadTimeActive) return null

        return StartResult.Blocked(
            badge = "Мертвяк",
            reason = "Таймер «$title» не запущен: пока активен «Мертвяк», новые таймеры навешивать нельзя.",
        )
    }

    private fun startHistoryNote(
        status: TrackedStatus,
        statusesClearedOnDeath: List<TrackedStatus> = emptyList(),
    ): String {
        if (status.presetId == DEAD_TIME_PRESET_ID) {
            val resetSummary = if (statusesClearedOnDeath.isEmpty()) {
                "Активных положительных и отрицательных эффектов не было."
            } else {
                "Сброшены эффекты: ${statusesClearedOnDeath.joinToString { "«${it.title}»" }}."
            }
            return "На вас наложен статус «${status.title}». $resetSummary Причина: смерть."
        }

        return when (status.category) {
            StatusCategory.DISEASE -> "Вы заразились болезнью «${status.title}»."
            StatusCategory.WOUND -> "Вы получили статус «${status.title}»."
            StatusCategory.POSITIVE -> "На вас наложен эффект «${status.title}»."
            StatusCategory.CONTROL -> "На вас наложен статус «${status.title}»."
            StatusCategory.CUSTOM -> "Запущен таймер «${status.title}»."
        }
    }

    private fun StatusCategory.isClearedOnDeath(): Boolean {
        return this == StatusCategory.DISEASE ||
            this == StatusCategory.WOUND ||
            this == StatusCategory.POSITIVE
    }
}
