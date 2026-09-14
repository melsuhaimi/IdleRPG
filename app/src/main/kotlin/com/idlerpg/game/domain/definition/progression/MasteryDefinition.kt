package com.idlerpg.game.domain.definition.progression

import com.idlerpg.game.core.id.ContentId
import com.idlerpg.game.core.number.GameNumber
import com.idlerpg.game.domain.definition.Affinity

/**
 * Authored affinity-mastery curve.
 *
 * Mastery experience is separate from Resonance charge: charge is transient combat language,
 * while mastery experience is run progression earned from effective affinity contribution.
 */
data class MasteryDefinition(
    val id: ContentId,
    val affinity: Affinity,
    val baseExperienceToNextLevel: GameNumber,
    val experienceIncrementPerLevel: GameNumber,
    val maxLevel: Long? = null
) {
    init {
        require(baseExperienceToNextLevel > GameNumber.ZERO) {
            "Mastery base experience must be > 0 for $id"
        }
        require(maxLevel == null || maxLevel >= 1L) {
            "Mastery maxLevel must be >= 1 when present for $id"
        }
    }
}
