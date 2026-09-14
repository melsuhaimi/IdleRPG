package com.idlerpg.game.domain.definition

import com.idlerpg.game.core.id.ContentId

/**
 * Canonical Resonance-affinity vocabulary.
 *
 * Foundation 5 defines the stable vocabulary only. Resonance charge, pulse generation,
 * sequence matching, and Convergence behavior remain locked until Foundation 8.
 */
enum class Affinity(
    val id: ContentId
) {
    MIGHT(ContentId("affinity.might")),
    TEMPO(ContentId("affinity.tempo")),
    EMBER(ContentId("affinity.ember")),
    FROST(ContentId("affinity.frost")),
    ARCANE(ContentId("affinity.arcane")),
    VITALITY(ContentId("affinity.vitality")),
    SHADOW(ContentId("affinity.shadow")),
    GUARD(ContentId("affinity.guard"))
}
