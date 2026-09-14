package com.idlerpg.game.domain.definition.progression

import com.idlerpg.game.core.id.ContentId

/** Reset scope for one authored feature unlock. */
enum class FeatureUnlockScope {
    RUN,
    META
}

/**
 * Stable authored conditions for unlocking one backend capability.
 *
 * Mastery requirements are keyed by canonical affinity ContentId rather than display text.
 */
data class FeatureUnlockDefinition(
    val id: ContentId,
    val scope: FeatureUnlockScope = FeatureUnlockScope.RUN,
    val requiredPlayerLevel: Long = 1L,
    val requiredMasteryLevels: Map<ContentId, Long> = emptyMap()
) {
    init {
        require(requiredPlayerLevel > 0L) {
            "requiredPlayerLevel must be positive for $id"
        }
        require(requiredMasteryLevels.values.all { it > 0L }) {
            "requiredMasteryLevels must contain only positive levels for $id"
        }
    }
}
