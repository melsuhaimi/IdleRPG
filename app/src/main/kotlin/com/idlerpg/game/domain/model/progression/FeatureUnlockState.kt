package com.idlerpg.game.domain.model.progression

import com.idlerpg.game.core.id.ContentId

/** Stable feature-unlock IDs for either run or meta scope, depending on its owner. */
data class FeatureUnlockState(
    val unlockedFeatureIds: Set<ContentId> = emptySet()
)
