package com.idlerpg.game.data.local.migration

import com.idlerpg.game.data.local.SaveData
import com.idlerpg.game.data.local.SaveDataException
import com.idlerpg.game.data.local.SaveVersion

/** Adds an empty deterministic skill-evolution selection map to predecessor saves. */
class V5ToV6SaveMigration : SaveMigration {
    override val fromVersion: SaveVersion = SaveVersion.V5
    override val toVersion: SaveVersion = SaveVersion.V6

    override fun migrate(data: SaveData): SaveData {
        val fields = data.fields.toMutableMap()
        val prefix = "run.player.selectedSkillEvolutionBySkillId"
        val collision = fields.keys.firstOrNull { it.startsWith(prefix) }
        if (collision != null) {
            throw SaveDataException("V5 save unexpectedly contains V6 field: $collision")
        }
        fields["$prefix.count"] = "0"
        return SaveData(fields)
    }
}
