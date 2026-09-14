package com.idlerpg.game.domain.command

import com.idlerpg.game.core.id.ContentId

/** Explicit player intent for persistent Echo-shop spending. */
sealed interface EchoCommand : GameCommand

data class PurchaseEchoOffer(
    val offerId: ContentId,
    override val correlationId: CommandCorrelationId? = null
) : EchoCommand
