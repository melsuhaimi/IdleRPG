package com.idlerpg.game.domain.model.resonance

import com.idlerpg.game.core.id.ContentId

/**
 * Ordered rolling affinity-symbol history.
 *
 * Foundation 8 keeps symbols as stable ContentIds in canonical state. The owning
 * ResonanceSequenceSystem enforces the configured maximum buffer size and deterministic
 * suffix matching against authored Affinity definitions.
 */
data class ResonanceSequenceState(
    val affinityIds: List<ContentId> = emptyList()
)
