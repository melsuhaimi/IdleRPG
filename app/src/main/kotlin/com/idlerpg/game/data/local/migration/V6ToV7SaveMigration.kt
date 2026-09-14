package com.idlerpg.game.data.local.migration

import com.idlerpg.game.data.local.SaveData
import com.idlerpg.game.data.local.SaveDataException
import com.idlerpg.game.data.local.SaveVersion

/** Adds an optional hero identity without invalidating or replacing old progress. */
class V6ToV7SaveMigration : SaveMigration {
    override val fromVersion: SaveVersion = SaveVersion.V6
    override val toVersion: SaveVersion = SaveVersion.V7

    override fun migrate(data: SaveData): SaveData {
        val fields = data.fields.toMutableMap()
        val prefix = "meta.heroName"
        val collision = fields.keys.firstOrNull { it.startsWith(prefix) }
        if (collision != null) {
            throw SaveDataException("V6 save unexpectedly contains V7 field: $collision")
        }
        fields["$prefix.present"] = "0"
        return SaveData(fields)
    }
}
