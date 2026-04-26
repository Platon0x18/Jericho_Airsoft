package com.masolodilov.jericho.data

import android.util.Base64
import com.masolodilov.jericho.model.InventoryCategory
import com.masolodilov.jericho.model.InventoryTransferPayload
import org.json.JSONObject

object InventoryTransferCodec {
    private const val PREFIX = "JERICHO_INV_1:"

    fun encode(payload: InventoryTransferPayload): String {
        val json = JSONObject()
            .put("transferId", payload.transferId)
            .put("title", payload.title)
            .put("category", payload.category.name)
            .put("quantity", payload.quantity)
            .put("createdAtMillis", payload.createdAtMillis)

        val encoded = Base64.encodeToString(
            json.toString().toByteArray(Charsets.UTF_8),
            Base64.URL_SAFE or Base64.NO_WRAP,
        )
        return PREFIX + encoded
    }

    fun decode(rawValue: String): InventoryTransferPayload? {
        val normalized = rawValue.trim()
        if (!normalized.startsWith(PREFIX)) return null

        return runCatching {
            val payload = normalized.removePrefix(PREFIX)
            val decoded = String(
                Base64.decode(payload, Base64.URL_SAFE or Base64.NO_WRAP),
                Charsets.UTF_8,
            )
            val json = JSONObject(decoded)
            InventoryTransferPayload(
                transferId = json.getString("transferId"),
                title = json.getString("title"),
                category = InventoryCategory.fromName(json.getString("category")),
                quantity = json.getInt("quantity"),
                createdAtMillis = json.getLong("createdAtMillis"),
            ).takeIf { item ->
                item.transferId.isNotBlank() &&
                    item.title.isNotBlank() &&
                    item.quantity > 0
            }
        }.getOrNull()
    }
}
