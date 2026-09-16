package com.idlerpg.game.domain.event

import com.idlerpg.game.core.number.GameNumber
import com.idlerpg.game.domain.model.rebirth.RebirthPointPool
import com.idlerpg.game.domain.model.rebirth.RebirthStat

/** Completed facts for permanent Rebirth progression and life resets. */
sealed interface RebirthEvent : GameEvent

data class RebirthPerformed(
    val rebirthNumber: Long,
    val previousLevel: Long,
    val goldCost: GameNumber,
    val normalPointsGranted: Long,
    val legacyPointsGranted: Long,
    val deepLevelReward: GameNumber = GameNumber.ZERO
) : RebirthEvent {
    init {
        require(rebirthNumber > 0L) { "rebirthNumber must be positive" }
        require(previousLevel >= 1L) { "previousLevel must be positive" }
        require(goldCost > GameNumber.ZERO) { "goldCost must be positive" }
        require(normalPointsGranted >= 0L) { "normalPointsGranted cannot be negative" }
        require(legacyPointsGranted >= 0L) { "legacyPointsGranted cannot be negative" }
        require(deepLevelReward >= GameNumber.ZERO) { "deepLevelReward cannot be negative" }
        require(
            normalPointsGranted > 0L || deepLevelReward > GameNumber.ZERO
        ) {
            "Rebirth must grant points or a deep-level reward"
        }
    }
}

data class RebirthPointsAllocated(
    val pool: RebirthPointPool,
    val stat: RebirthStat,
    val amount: Long,
    val remainingPoints: Long
) : RebirthEvent {
    init {
        require(amount > 0L) { "amount must be positive" }
        require(remainingPoints >= 0L) { "remainingPoints cannot be negative" }
    }
}

data class RebirthAllocationsReset(
    val pool: RebirthPointPool,
    val gemCost: GameNumber
) : RebirthEvent {
    init {
        require(gemCost > GameNumber.ZERO) { "gemCost must be positive" }
    }
}
