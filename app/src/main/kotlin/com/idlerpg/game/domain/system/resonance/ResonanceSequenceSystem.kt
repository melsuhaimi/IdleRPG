package com.idlerpg.game.domain.system.resonance

import com.idlerpg.game.domain.definition.Affinity
import com.idlerpg.game.domain.definition.resonance.ResonancePatternDefinition
import com.idlerpg.game.domain.model.resonance.ResonanceSequenceState

/** Owns bounded deterministic affinity-sequence mutation and suffix matching. */
object ResonanceSequenceSystem {

    fun append(
        state: ResonanceSequenceState,
        affinity: Affinity,
        maximumSize: Int
    ): ResonanceSequenceState {
        require(maximumSize > 0) {
            "maximumSize must be positive: $maximumSize"
        }

        val appended = state.affinityIds + affinity.id
        val bounded =
            if (appended.size > maximumSize) {
                appended.takeLast(maximumSize)
            } else {
                appended
            }

        return ResonanceSequenceState(affinityIds = bounded)
    }

    fun matchesSuffix(
        state: ResonanceSequenceState,
        pattern: ResonancePatternDefinition
    ): Boolean {
        if (pattern.length > state.affinityIds.size) {
            return false
        }

        val expected = pattern.affinities.map { it.id }
        return state.affinityIds.takeLast(expected.size) == expected
    }
}
