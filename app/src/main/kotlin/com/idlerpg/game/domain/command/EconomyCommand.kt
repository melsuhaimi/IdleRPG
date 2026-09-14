package com.idlerpg.game.domain.command

import com.idlerpg.game.core.id.ContentId

/** Economy-related player intent. Implementation belongs to later economy systems. */
sealed interface EconomyCommand : GameCommand

/** Attempt to purchase one or more levels of one stable upgrade definition. */
data class PurchaseUpgrade(
    val upgradeId: ContentId,
    val quantity: Long = 1L,
    override val correlationId: CommandCorrelationId? = null
) : EconomyCommand {
    init {
        require(quantity > 0L) { "PurchaseUpgrade.quantity must be positive: $quantity" }
    }
}
