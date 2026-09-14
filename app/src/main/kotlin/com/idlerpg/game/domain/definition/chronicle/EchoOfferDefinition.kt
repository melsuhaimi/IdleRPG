package com.idlerpg.game.domain.definition.chronicle

import com.idlerpg.game.core.id.ContentId
import com.idlerpg.game.core.number.GameNumber

/**
 * Authored persistent Echo-shop offer.
 *
 * Effects deliberately reuse the bounded persistent-effect vocabulary introduced for
 * Foundation 17. FBE-03 supports nonrepeatable offers only.
 */
data class EchoOfferDefinition(
    val id: ContentId,
    val cost: GameNumber,
    val effects: List<EchoUnlockEffect>,
    val requiredOfferIds: Set<ContentId> = emptySet(),
    val repeatable: Boolean = false
) {
    init {
        require(cost > GameNumber.ZERO) {
            "Echo offer cost must be > 0 for $id"
        }
        require(effects.isNotEmpty()) {
            "Echo offer $id must contain at least one effect"
        }
        require(id !in requiredOfferIds) {
            "Echo offer $id cannot require itself"
        }
    }
}
