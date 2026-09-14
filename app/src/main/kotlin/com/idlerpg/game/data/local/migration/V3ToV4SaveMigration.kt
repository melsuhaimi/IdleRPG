package com.idlerpg.game.data.local.migration

import com.idlerpg.game.data.local.SaveData
import com.idlerpg.game.data.local.SaveVersion

/** Enables safe default auto-salvage for V3 saves without touching item ownership. */
class V3ToV4SaveMigration : SaveMigration {
    override val fromVersion: SaveVersion = SaveVersion.V3
    override val toVersion: SaveVersion = SaveVersion.V4

    override fun migrate(data: SaveData): SaveData {
        val fields = data.fields.toMutableMap()
        fields.putIfAbsent("run.inventory.lootFilter.autoSalvageEnabled", "1")
        fields.putIfAbsent("run.inventory.lootFilter.minimumKeepRarity", "rarity.rare")
        return SaveData(fields)
    }
}
