package com.idlerpg.game.domain.definition.resonance

import com.idlerpg.game.domain.definition.Affinity

/** Ordered affinity suffix required to trigger a Convergence. */
data class ResonancePatternDefinition(
    val affinities: List<Affinity>
) {
    init {
        require(affinities.isNotEmpty()) {
            "ResonancePatternDefinition.affinities cannot be empty"
        }
    }

    val length: Int
        get() = affinities.size
}
