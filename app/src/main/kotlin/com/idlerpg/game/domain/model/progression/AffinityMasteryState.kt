package com.idlerpg.game.domain.model.progression

import com.idlerpg.game.core.id.ContentId
import com.idlerpg.game.core.number.GameNumber

/**
 * Canonical run-level affinity-mastery experience.
 *
 * Mastery level is derived from this experience and MasteryDefinition; it is not duplicated.
 */
data class AffinityMasteryState(
    val experienceByAffinityId: Map<ContentId, GameNumber> = emptyMap()
)
