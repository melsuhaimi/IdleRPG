package com.idlerpg.game.data.local.migration

import com.idlerpg.game.data.local.SaveData
import com.idlerpg.game.data.local.SaveDataException
import com.idlerpg.game.data.local.SaveVersion

/** Adds persisted wave position to V2 encounters while preserving their single-wave state. */
class V2ToV3SaveMigration : SaveMigration {
    override val fromVersion: SaveVersion = SaveVersion.V2
    override val toVersion: SaveVersion = SaveVersion.V3

    override fun migrate(data: SaveData): SaveData {
        val fields = data.fields.toMutableMap()
        val presentKey = "run.world.currentEncounter.present"
        val present = fields[presentKey]
            ?: throw SaveDataException("Missing required V2 field: $presentKey")
        if (present != "0" && present != "1") {
            throw SaveDataException("Invalid V2 boolean '$present' at $presentKey")
        }
        if (present == "1") {
            val waveKey = "run.world.currentEncounter.currentWave"
            if (fields.putIfAbsent(waveKey, "1") != null) {
                throw SaveDataException("V2 save unexpectedly contains V3 field: $waveKey")
            }
        }
        return SaveData(fields)
    }
}
