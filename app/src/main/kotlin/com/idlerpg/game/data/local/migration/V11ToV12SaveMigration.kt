package com.idlerpg.game.data.local.migration

import com.idlerpg.game.data.local.SaveData
import com.idlerpg.game.data.local.SaveDataException
import com.idlerpg.game.data.local.SaveVersion

/** Adds the zero-valued Defense rating to persisted player base stats. */
class V11ToV12SaveMigration : SaveMigration {
    override val fromVersion = SaveVersion.V11
    override val toVersion = SaveVersion.V12

    override fun migrate(data: SaveData): SaveData {
        val field = "run.player.baseStats.defense"
        val storedValue = data.fields[field]
        if (storedValue != null && storedValue != "0") {
            throw SaveDataException("V11 save unexpectedly contains V12 field: $field")
        }
        return if (storedValue == null) SaveData(data.fields + (field to "0")) else data
    }
}
