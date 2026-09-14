package com.idlerpg.game.domain.definition.economy

import com.idlerpg.game.core.id.ContentId
import com.idlerpg.game.core.number.GameNumber

/**
 * Authored deterministic upgrade-cost curve.
 *
 * Foundation 6 intentionally implements only the Linear curve required by the verified
 * prototype. Additional formula families are added only when a later design needs them.
 */
sealed interface CostFormulaDefinition {
    val id: ContentId

    data class Linear(
        override val id: ContentId,
        val baseCost: GameNumber,
        val incrementPerLevel: GameNumber
    ) : CostFormulaDefinition {
        init {
            require(baseCost > GameNumber.ZERO) {
                "Linear baseCost must be greater than zero for $id"
            }
        }
    }
}
