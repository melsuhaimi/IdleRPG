package com.idlerpg.game.domain.model.world

import com.idlerpg.game.core.id.ContentId
import com.idlerpg.game.core.number.GameNumber

/** Mutable progression belonging to one region; the containing map owns the region ID. */
data class RegionProgressState(
    val highestClearedEncounterTier: Long = 0L,
    val normalClears: GameNumber = GameNumber.ZERO,
    val eliteClears: GameNumber = GameNumber.ZERO,
    val clearedBossIds: Set<ContentId> = emptySet(),
    val discoveryFlagIds: Set<ContentId> = emptySet()
) {
    init {
        require(highestClearedEncounterTier >= 0L) {
            "highestClearedEncounterTier cannot be negative: $highestClearedEncounterTier"
        }
    }
}
