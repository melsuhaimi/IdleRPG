package com.idlerpg.game.domain.command

import com.idlerpg.game.domain.model.rebirth.RebirthPointPool
import com.idlerpg.game.domain.model.rebirth.RebirthStat

/** Permanent Rebirth progression and soft-reset player intent. */
sealed interface RebirthCommand : GameCommand

/** Spend the next Rebirth cost and start a level-one life atomically. */
data class PerformRebirth(
    override val correlationId: CommandCorrelationId? = null
) : RebirthCommand

/** Assign earned points to one permanent base-stat channel. */
data class AllocateRebirthPoints(
    val pool: RebirthPointPool,
    val stat: RebirthStat,
    val amount: Long,
    override val correlationId: CommandCorrelationId? = null
) : RebirthCommand {
    init {
        require(amount > 0L) { "Rebirth allocation amount must be positive: $amount" }
    }
}

/** Buy a gem-backed respec for one permanent point pool. */
data class ResetRebirthAllocations(
    val pool: RebirthPointPool,
    override val correlationId: CommandCorrelationId? = null
) : RebirthCommand
