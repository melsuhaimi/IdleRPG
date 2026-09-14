package com.idlerpg.game.domain.system.chronicle

import com.idlerpg.game.core.id.ContentId
import com.idlerpg.game.core.number.GameNumber
import com.idlerpg.game.domain.event.EchoGranted
import com.idlerpg.game.domain.event.GameEvent
import com.idlerpg.game.domain.model.MetaState
import com.idlerpg.game.domain.model.chronicle.EchoState

/** Persistent Echo accounting. Spending is owned by EchoOfferSystem. */
object EchoSystem {

    data class GrantResult(
        val meta: MetaState,
        val events: List<GameEvent>
    )

    fun lifetimeEarned(echoes: EchoState): GameNumber =
        echoes.available + echoes.spent

    fun grant(
        meta: MetaState,
        amount: GameNumber,
        sourceId: ContentId?
    ): GrantResult {
        require(amount > GameNumber.ZERO) {
            "Echo grant amount must be > 0"
        }

        val current = meta.copy(
            echoes = meta.echoes.copy(
                available = meta.echoes.available + amount
            )
        )

        return GrantResult(
            meta = current,
            events = listOf(
                EchoGranted(
                    amount = amount,
                    sourceId = sourceId
                )
            )
        )
    }
}
