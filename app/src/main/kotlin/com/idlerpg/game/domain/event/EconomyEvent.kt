package com.idlerpg.game.domain.event

import com.idlerpg.game.core.id.ContentId
import com.idlerpg.game.core.number.GameNumber
import com.idlerpg.game.domain.command.CommandRejectionReason
import com.idlerpg.game.domain.definition.CurrencyId

/** Completed economy-domain facts. */
sealed interface EconomyEvent : GameEvent

data class CurrencyGranted(
    val currencyId: CurrencyId,
    val amount: GameNumber,
    val sourceId: ContentId? = null
) : EconomyEvent

data class CurrencySpent(
    val currencyId: CurrencyId,
    val amount: GameNumber,
    val purposeId: ContentId? = null
) : EconomyEvent

data class UpgradePurchased(
    val upgradeId: ContentId,
    val quantity: Long,
    val newLevel: Long,
    val currencyId: CurrencyId? = null,
    val totalCost: GameNumber = GameNumber.ZERO
) : EconomyEvent {
    init {
        require(quantity > 0L) { "UpgradePurchased.quantity must be positive: $quantity" }
        require(newLevel >= 0L) { "UpgradePurchased.newLevel cannot be negative: $newLevel" }
    }
}

data class PurchaseRejected(
    val upgradeId: ContentId,
    val reason: CommandRejectionReason
) : EconomyEvent
