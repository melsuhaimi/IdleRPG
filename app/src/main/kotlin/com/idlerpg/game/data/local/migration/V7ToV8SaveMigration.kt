package com.idlerpg.game.data.local.migration

import com.idlerpg.game.data.local.SaveData
import com.idlerpg.game.data.local.SaveDataException
import com.idlerpg.game.data.local.SaveVersion

/** Adds the permanent Rebirth/Legacy point pools to a V7 save. */
class V7ToV8SaveMigration : SaveMigration {
    override val fromVersion: SaveVersion = SaveVersion.V7
    override val toVersion: SaveVersion = SaveVersion.V8

    override fun migrate(data: SaveData): SaveData {
        val fields = data.fields.toMutableMap()
        val prefix = "meta.rebirth"
        val collision = fields.keys.firstOrNull { it.startsWith(prefix) }
        if (collision != null) {
            throw SaveDataException("V7 save unexpectedly contains V8 field: $collision")
        }
        fields["$prefix.completedRebirths"] = "0"
        fields["$prefix.normalPointsEarned"] = "0"
        fields["$prefix.legacyPointsEarned"] = "0"
        fields["$prefix.normalAllocations.count"] = "0"
        fields["$prefix.legacyAllocations.count"] = "0"
        return SaveData(fields)
    }
}
