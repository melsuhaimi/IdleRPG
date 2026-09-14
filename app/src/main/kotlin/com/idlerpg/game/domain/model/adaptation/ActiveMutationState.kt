package com.idlerpg.game.domain.model.adaptation

import com.idlerpg.game.core.id.ContentId

/**
 * Runtime snapshot of one Adaptation mutation applied when an enemy was spawned.
 *
 * The mutation definition remains static content. The triggering affinity and tier are
 * snapshotted so an already-spawned enemy does not change if regional pressure changes
 * later in the same encounter.
 */
data class ActiveMutationState(
    val mutationId: ContentId,
    val sourceAffinityId: ContentId,
    val adaptationTier: Int
) {
    init {
        require(adaptationTier > 0) {
            "ActiveMutationState.adaptationTier must be positive: $adaptationTier"
        }
    }
}
