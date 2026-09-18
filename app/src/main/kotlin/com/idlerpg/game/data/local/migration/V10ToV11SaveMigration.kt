package com.idlerpg.game.data.local.migration

import com.idlerpg.game.data.local.SaveData
import com.idlerpg.game.data.local.SaveVersion
import com.idlerpg.game.domain.model.rebirth.RebirthPointPool

/** Refund incompatible investments to their original pool; earned totals never change. */
class V10ToV11SaveMigration : SaveMigration {
    override val fromVersion = SaveVersion.V10
    override val toVersion = SaveVersion.V11

    override fun migrate(data: SaveData): SaveData {
        val state = data.toGameState()
        val rebirth = state.meta.rebirth
        return SaveData.fromGameState(state.copy(meta = state.meta.copy(
            rebirth = rebirth.copy(
                normalAllocations = rebirth.normalAllocations.filterKeys { it.supports(RebirthPointPool.NORMAL) },
                legacyAllocations = rebirth.legacyAllocations.filterKeys { it.supports(RebirthPointPool.LEGACY) }
            )
        )))
    }
}
