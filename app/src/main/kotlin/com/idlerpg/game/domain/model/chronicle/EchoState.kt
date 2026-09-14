package com.idlerpg.game.domain.model.chronicle

import com.idlerpg.game.core.id.ContentId
import com.idlerpg.game.core.number.GameNumber

/** Persistent Echo resource/progression quantities and purchased-offer identity. */
data class EchoState(
    val available: GameNumber = GameNumber.ZERO,
    val spent: GameNumber = GameNumber.ZERO,
    val purchasedOfferIds: Set<ContentId> = emptySet()
) {
    init {
        require(available >= GameNumber.ZERO) {
            "Echo available cannot be negative: $available"
        }
        require(spent >= GameNumber.ZERO) {
            "Echo spent cannot be negative: $spent"
        }
    }
}
