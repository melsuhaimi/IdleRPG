package com.idlerpg.game.domain.model.adaptation

import com.idlerpg.game.core.id.ContentId

/** Root run-level world-adaptation state keyed by stable region ID. */
data class AdaptationState(
    val regionStateById: Map<ContentId, RegionAdaptationState> = emptyMap()
)
