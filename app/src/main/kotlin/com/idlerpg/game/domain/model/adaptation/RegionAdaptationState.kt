package com.idlerpg.game.domain.model.adaptation

import com.idlerpg.game.core.id.ContentId

/**
 * Mutable ecological Adaptation state belonging to one region.
 *
 * Pressure and per-encounter contribution are grouped in [AffinityExposureState]. Tiers
 * are explicit derived progression snapshots so future enemy generation can be resumed
 * without depending on unordered recalculation. [unlockedMutationIds] is bounded to
 * currently tier-eligible authored mutation IDs.
 */
data class RegionAdaptationState(
    val exposureByAffinityId: Map<ContentId, AffinityExposureState> = emptyMap(),
    val tierByAffinityId: Map<ContentId, Int> = emptyMap(),
    val unlockedMutationIds: Set<ContentId> = emptySet()
) {
    init {
        require(tierByAffinityId.values.all { it >= 0 }) {
            "Adaptation tiers cannot be negative"
        }
    }
}
