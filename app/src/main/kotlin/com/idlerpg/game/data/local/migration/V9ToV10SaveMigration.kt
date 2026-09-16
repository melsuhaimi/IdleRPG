package com.idlerpg.game.data.local.migration

import com.idlerpg.game.data.local.SaveData
import com.idlerpg.game.data.local.SaveDataException
import com.idlerpg.game.data.local.SaveVersion

/** Adds zeroed enhancement failstacks to every item in a V9 save. */
class V9ToV10SaveMigration : SaveMigration {
    override val fromVersion: SaveVersion = SaveVersion.V9
    override val toVersion: SaveVersion = SaveVersion.V10

    override fun migrate(data: SaveData): SaveData {
        val fields = data.fields.toMutableMap()
        val itemPrefixes = fields.keys
            .filter { key ->
                key.endsWith(".enhancementLevel") &&
                    (key.startsWith("run.inventory.itemsById.") ||
                        key.startsWith("run.inventory.overflowItemsById."))
            }
            .map { it.removeSuffix(".enhancementLevel") }
            .toSet()
        itemPrefixes.forEach { itemPrefix ->
            val failstackPath = "$itemPrefix.enhancementFailstack"
            if (failstackPath in fields) {
                throw SaveDataException(
                    "V9 save unexpectedly contains V10 field: $failstackPath"
                )
            }
            fields[failstackPath] = "0"
        }
        return SaveData(fields)
    }
}
