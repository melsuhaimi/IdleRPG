package com.idlerpg.game.domain.definition.adaptation

import com.idlerpg.game.core.id.ContentId
import com.idlerpg.game.core.number.Ratio

/**
 * Reward compensation for enemies carrying active Adaptation mutations.
 *
 * The multiplier is derived from active mutation count; it is never stored as canonical
 * gameplay state.
 */
data class AdaptationRewardDefinition(
    val id: ContentId,
    val bonusPerActiveMutation: Ratio,
    val maximumMultiplier: Ratio
) {
    init {
        require(maximumMultiplier >= Ratio.ONE) {
            "Adaptation reward maximumMultiplier must be at least 100% for $id"
        }
    }
}
