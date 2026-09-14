package com.idlerpg.game.domain.definition.resonance

import com.idlerpg.game.core.number.GameNumber
import com.idlerpg.game.domain.definition.Affinity

/**
 * One authored Resonance pulse emitted by a combat action.
 *
 * [amount] contributes charge to the emitted [affinity]. The sequence system records one
 * affinity symbol per emission definition regardless of amount, so potency and ordering
 * remain separate concepts.
 */
data class ResonanceEmissionDefinition(
    val affinity: Affinity,
    val amount: GameNumber = GameNumber.ONE
) {
    init {
        require(amount > GameNumber.ZERO) {
            "ResonanceEmissionDefinition.amount must be > 0"
        }
    }
}
