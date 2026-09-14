package com.idlerpg.game.data.local.migration

import com.idlerpg.game.core.config.BalanceConfig
import com.idlerpg.game.data.local.SaveData
import com.idlerpg.game.data.local.SaveDataException
import com.idlerpg.game.data.local.SaveVersion

/**
 * Exact sequential migration from the accepted Foundation-15/18 V1 field contract to V2.
 *
 * Historical V1 semantics matter:
 * - completed quest rewards were already auto-granted;
 * - completed achievement rewards were already auto-granted;
 * - Adaptation Forecast could already have been revealed automatically by lifetime Echo;
 * - inventory had no capacity/overflow fields;
 * - manual combat queue did not exist.
 *
 * The migration therefore initializes V2 state without duplicating rewards, charging Echo,
 * or deleting/moving existing items.
 */
class V1ToV2SaveMigration(
    private val baseInventoryCapacity: Long =
        BalanceConfig.DEFAULT_BASE_INVENTORY_CAPACITY
) : SaveMigration {

    override val fromVersion: SaveVersion = SaveVersion.V1
    override val toVersion: SaveVersion = SaveVersion.V2

    init {
        require(baseInventoryCapacity > 0L) {
            "baseInventoryCapacity must be positive: $baseInventoryCapacity"
        }
    }

    override fun migrate(data: SaveData): SaveData {
        val fields = data.fields.toMutableMap()

        putNew(fields, "run.combat.queuedPlayerAction.present", "0")

        val questCount = readCount(fields, "run.quests")
        repeat(questCount) { index ->
            val progressPath = "run.quests.$index.progress"
            val completionCount = required(fields, "$progressPath.completionCount")
            putNew(fields, "$progressPath.claimedCount", completionCount)
        }

        val achievementCount = readCount(fields, "meta.achievements")
        repeat(achievementCount) { index ->
            val progressPath = "meta.achievements.$index.progress"
            val completed = required(fields, "$progressPath.completed")
            if (completed != "0" && completed != "1") {
                throw SaveDataException(
                    "Invalid V1 achievement completed boolean '$completed' at " +
                        "$progressPath.completed"
                )
            }
            putNew(fields, "$progressPath.rewardClaimed", completed)
        }

        val adaptationForecastAlreadyApplied =
            containsContentId(
                fields = fields,
                path = "meta.discoveries.unlockedHiddenContentIds",
                expected = ADAPTATION_FORECAST_DISCOVERY_ID
            )

        if (adaptationForecastAlreadyApplied) {
            putNew(fields, "meta.echoes.purchasedOfferIds.count", "1")
            putNew(
                fields,
                "meta.echoes.purchasedOfferIds.0",
                ADAPTATION_FORECAST_ECHO_OFFER_ID
            )
        } else {
            putNew(fields, "meta.echoes.purchasedOfferIds.count", "0")
        }

        val existingItemCount = readCount(fields, "run.inventory.itemsById")
        val migratedCapacity = maxOf(
            baseInventoryCapacity,
            existingItemCount.toLong()
        )
        putNew(fields, "run.inventory.slotCapacity", migratedCapacity.toString())
        putNew(fields, "run.inventory.capacityUpgradePurchases", "0")
        putNew(fields, "run.inventory.overflowItemsById.count", "0")

        return SaveData(fields)
    }

    private fun containsContentId(
        fields: Map<String, String>,
        path: String,
        expected: String
    ): Boolean {
        val count = readCount(fields, path)
        repeat(count) { index ->
            if (required(fields, "$path.$index") == expected) {
                return true
            }
        }
        return false
    }

    private fun readCount(
        fields: Map<String, String>,
        path: String
    ): Int {
        val key = "$path.count"
        val raw = required(fields, key)
        val value = raw.toIntOrNull()
            ?: throw SaveDataException(
                "Invalid V1 collection count '$raw' at $key"
            )
        if (value < 0) {
            throw SaveDataException(
                "Negative V1 collection count at $key: $value"
            )
        }
        if (value > MAX_MIGRATION_COLLECTION_ENTRIES) {
            throw SaveDataException(
                "V1 collection count exceeds migration safety limit at $key: $value"
            )
        }
        return value
    }

    private fun required(
        fields: Map<String, String>,
        key: String
    ): String =
        fields[key]
            ?: throw SaveDataException(
                "Missing required V1 SaveData field during V1 -> V2 migration: $key"
            )

    private fun putNew(
        fields: MutableMap<String, String>,
        key: String,
        value: String
    ) {
        if (fields.containsKey(key)) {
            throw SaveDataException(
                "V1 save unexpectedly already contains V2 field: $key"
            )
        }
        fields[key] = value
    }

    companion object {
        private const val MAX_MIGRATION_COLLECTION_ENTRIES: Int = 100_000
        private const val ADAPTATION_FORECAST_DISCOVERY_ID: String =
            "discovery.adaptation_forecast"
        private const val ADAPTATION_FORECAST_ECHO_OFFER_ID: String =
            "echo_unlock.adaptation_forecast"
    }
}
