package com.idlerpg.game.domain.model.progression

import com.idlerpg.game.core.number.GameNumber

/** Canonical run-level player level and unspent experience toward the next level. */
data class PlayerLevelState(
    val level: Long = 1L,
    val currentExperience: GameNumber = GameNumber.ZERO
) {
    init {
        require(level > 0L) { "Player level must be positive: $level" }
    }
}
