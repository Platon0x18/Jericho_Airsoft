package com.masolodilov.jericho.data

import android.content.Context
import com.masolodilov.jericho.model.EffectTag
import com.masolodilov.jericho.model.InventoryAction
import com.masolodilov.jericho.model.InventoryCategory
import com.masolodilov.jericho.model.InventoryItem
import com.masolodilov.jericho.model.InventoryLogItem
import com.masolodilov.jericho.model.PlayerProfile
import com.masolodilov.jericho.model.StatusCategory
import com.masolodilov.jericho.model.StatusHistoryItem
import com.masolodilov.jericho.model.StatusOutcome
import com.masolodilov.jericho.model.TrackedStatus
import org.json.JSONArray
import org.json.JSONObject

class StatusStorage(context: Context) {
    private val preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun loadActiveStatuses(): List<TrackedStatus> {
        val raw = preferences.getString(KEY_ACTIVE, null) ?: return emptyList()
        return runCatching {
            val json = JSONArray(raw)
            buildList {
                for (index in 0 until json.length()) {
                    val item = json.getJSONObject(index)
                    add(
                        TrackedStatus(
                            id = item.getString("id"),
                            presetId = item.optString("presetId").takeIf {
                                item.has("presetId") && !item.isNull("presetId") && it.isNotBlank()
                            },
                            title = item.getString("title"),
                            category = StatusCategory.fromName(item.getString("category")),
                            durationMillis = item.getLong("durationMillis"),
                            startedAtMillis = item.getLong("startedAtMillis"),
                            pausedAtMillis = if (item.isNull("pausedAtMillis")) null else item.getLong("pausedAtMillis"),
                            totalPausedMillis = item.getLong("totalPausedMillis"),
                            description = item.optString("description"),
                            grantedEffects = EffectTag.decodeList(
                                item.optString("grantedEffects").takeIf { it.isNotBlank() },
                            ),
                        ),
                    )
                }
            }
        }.getOrDefault(emptyList())
    }

    fun loadHistoryItems(): List<StatusHistoryItem> {
        val raw = preferences.getString(KEY_HISTORY, null) ?: return emptyList()
        return runCatching {
            val json = JSONArray(raw)
            buildList {
                for (index in 0 until json.length()) {
                    val item = json.getJSONObject(index)
                    add(
                        StatusHistoryItem(
                            id = item.getString("id"),
                            title = item.getString("title"),
                            category = StatusCategory.fromName(item.getString("category")),
                            durationMillis = item.getLong("durationMillis"),
                            finishedAtMillis = item.getLong("finishedAtMillis"),
                            outcome = StatusOutcome.fromName(item.getString("outcome")),
                            note = item.optString("note").takeIf {
                                item.has("note") && !item.isNull("note") && it.isNotBlank()
                            },
                        ),
                    )
                }
            }
        }.getOrDefault(emptyList())
    }

    fun loadEnabledPermanentEffectIds(): Set<String> {
        val raw = preferences.getString(KEY_PERMANENT_EFFECTS, null) ?: return emptySet()
        return runCatching {
            val json = JSONArray(raw)
            buildSet {
                for (index in 0 until json.length()) {
                    add(json.getString(index))
                }
            }
        }.getOrDefault(emptySet())
    }

    fun loadInventoryItems(): List<InventoryItem> {
        val raw = preferences.getString(KEY_INVENTORY_ITEMS, null) ?: return emptyList()
        return runCatching {
            val json = JSONArray(raw)
            buildList {
                for (index in 0 until json.length()) {
                    val item = json.getJSONObject(index)
                    add(
                        InventoryItem(
                            id = item.getString("id"),
                            title = item.getString("title"),
                            category = InventoryCategory.fromName(item.getString("category")),
                            quantity = item.getInt("quantity"),
                        ),
                    )
                }
            }
        }.getOrDefault(emptyList())
    }

    fun loadInventoryLogItems(): List<InventoryLogItem> {
        val raw = preferences.getString(KEY_INVENTORY_LOG, null) ?: return emptyList()
        return runCatching {
            val json = JSONArray(raw)
            buildList {
                for (index in 0 until json.length()) {
                    val item = json.getJSONObject(index)
                    add(
                        InventoryLogItem(
                            id = item.getString("id"),
                            title = item.getString("title"),
                            category = InventoryCategory.fromName(item.getString("category")),
                            action = InventoryAction.fromName(item.getString("action")),
                            quantity = item.getInt("quantity"),
                            totalAfter = item.getInt("totalAfter"),
                            happenedAtMillis = item.getLong("happenedAtMillis"),
                            note = item.optString("note").takeIf {
                                item.has("note") && !item.isNull("note") && it.isNotBlank()
                            },
                        ),
                    )
                }
            }
        }.getOrDefault(emptyList())
    }

    fun loadPlayerProfile(): PlayerProfile {
        val raw = preferences.getString(KEY_PLAYER_PROFILE, null) ?: return PlayerProfile()
        return runCatching {
            val item = JSONObject(raw)
            PlayerProfile(
                faction = item.optString("faction"),
                callsign = item.optString("callsign"),
                bloodType = item.optString("bloodType"),
                allergy = item.optString("allergy"),
                specialNotes = item.optString("specialNotes"),
            )
        }.getOrDefault(PlayerProfile())
    }

    fun loadReceivedTransferIds(): Set<String> {
        val raw = preferences.getString(KEY_RECEIVED_TRANSFER_IDS, null) ?: return emptySet()
        return runCatching {
            val json = JSONArray(raw)
            buildSet {
                for (index in 0 until json.length()) {
                    add(json.getString(index))
                }
            }
        }.getOrDefault(emptySet())
    }

    fun save(
        activeStatuses: List<TrackedStatus>,
        historyItems: List<StatusHistoryItem>,
        enabledPermanentEffectIds: Set<String>,
        inventoryItems: List<InventoryItem>,
        inventoryLogItems: List<InventoryLogItem>,
        receivedTransferIds: Set<String>,
        playerProfile: PlayerProfile,
    ) {
        preferences.edit()
            .putString(KEY_ACTIVE, activeStatuses.toActiveStatusesJson())
            .putString(KEY_HISTORY, historyItems.toHistoryItemsJson())
            .putString(KEY_PERMANENT_EFFECTS, enabledPermanentEffectIds.toPermanentEffectsJson())
            .putString(KEY_INVENTORY_ITEMS, inventoryItems.toInventoryItemsJson())
            .putString(KEY_INVENTORY_LOG, inventoryLogItems.toInventoryLogJson())
            .putString(KEY_RECEIVED_TRANSFER_IDS, receivedTransferIds.toReceivedTransferIdsJson())
            .putString(KEY_PLAYER_PROFILE, playerProfile.toJson())
            .apply()
    }

    private fun List<TrackedStatus>.toActiveStatusesJson(): String {
        val array = JSONArray()
        forEach { status ->
            array.put(
                JSONObject()
                    .put("id", status.id)
                    .put("presetId", status.presetId)
                    .put("title", status.title)
                    .put("category", status.category.name)
                    .put("durationMillis", status.durationMillis)
                    .put("startedAtMillis", status.startedAtMillis)
                    .put("pausedAtMillis", status.pausedAtMillis)
                    .put("totalPausedMillis", status.totalPausedMillis)
                    .put("description", status.description)
                    .put("grantedEffects", EffectTag.encodeList(status.grantedEffects)),
            )
        }
        return array.toString()
    }

    private fun List<StatusHistoryItem>.toHistoryItemsJson(): String {
        val array = JSONArray()
        forEach { item ->
            array.put(
                JSONObject()
                    .put("id", item.id)
                    .put("title", item.title)
                    .put("category", item.category.name)
                    .put("durationMillis", item.durationMillis)
                    .put("finishedAtMillis", item.finishedAtMillis)
                    .put("outcome", item.outcome.name)
                    .put("note", item.note),
            )
        }
        return array.toString()
    }

    private fun Set<String>.toPermanentEffectsJson(): String {
        val array = JSONArray()
        forEach(array::put)
        return array.toString()
    }

    private fun List<InventoryItem>.toInventoryItemsJson(): String {
        val array = JSONArray()
        forEach { item ->
            array.put(
                JSONObject()
                    .put("id", item.id)
                    .put("title", item.title)
                    .put("category", item.category.name)
                    .put("quantity", item.quantity),
            )
        }
        return array.toString()
    }

    private fun List<InventoryLogItem>.toInventoryLogJson(): String {
        val array = JSONArray()
        forEach { item ->
            array.put(
                JSONObject()
                    .put("id", item.id)
                    .put("title", item.title)
                    .put("category", item.category.name)
                    .put("action", item.action.name)
                    .put("quantity", item.quantity)
                    .put("totalAfter", item.totalAfter)
                    .put("happenedAtMillis", item.happenedAtMillis)
                    .put("note", item.note),
            )
        }
        return array.toString()
    }

    private fun Set<String>.toReceivedTransferIdsJson(): String {
        val array = JSONArray()
        forEach(array::put)
        return array.toString()
    }

    private fun PlayerProfile.toJson(): String {
        return JSONObject()
            .put("faction", faction)
            .put("callsign", callsign)
            .put("bloodType", bloodType)
            .put("allergy", allergy)
            .put("specialNotes", specialNotes)
            .toString()
    }

    private companion object {
        const val PREFS_NAME = "jericho_status_prefs"
        const val KEY_ACTIVE = "active_statuses"
        const val KEY_HISTORY = "history_statuses"
        const val KEY_PERMANENT_EFFECTS = "permanent_effects"
        const val KEY_INVENTORY_ITEMS = "inventory_items"
        const val KEY_INVENTORY_LOG = "inventory_log"
        const val KEY_RECEIVED_TRANSFER_IDS = "received_transfer_ids"
        const val KEY_PLAYER_PROFILE = "player_profile"
    }
}
