package com.idlerpg.game.data.local.migration

import com.idlerpg.game.data.local.SaveData
import com.idlerpg.game.data.local.SaveDataException
import com.idlerpg.game.data.local.SaveVersion

/** Adds empty run-scoped per-skill rank, mastery, and refinement maps to a V8 save. */
class V8ToV9SaveMigration : SaveMigration {
    override val fromVersion: SaveVersion = SaveVersion.V8
    override val toVersion: SaveVersion = SaveVersion.V9

    override fun migrate(data: SaveData): SaveData {
        val fields = data.fields.toMutableMap()
        val prefix = "run.progression.skillProgression"
        val collision = fields.keys.firstOrNull { it.startsWith(prefix) }
        if (collision != null) {
            throw SaveDataException("V8 save unexpectedly contains V9 field: " + collision)
        }
        fields["$prefix.rankBySkillId.count"] = "0"
        fields["$prefix.masteryBySkillId.count"] = "0"
        fields["$prefix.refinementBySkillId.count"] = "0"
        return SaveData(fields)
    }
}
