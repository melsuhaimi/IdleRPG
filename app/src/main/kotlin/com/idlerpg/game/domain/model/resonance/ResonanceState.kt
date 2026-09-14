package com.idlerpg.game.domain.model.resonance

import com.idlerpg.game.core.id.ContentId
import com.idlerpg.game.core.number.GameNumber

/**
 * Canonical run-local Resonance state.
 *
 * Charge and rolling sequence reset/persistence policy belongs to the owning Resonance
 * systems. Permanent Convergence discovery is deliberately not stored here; it belongs
 * to MetaState.discovery.
 */
data class ResonanceState(
    val chargeByAffinityId: Map<ContentId, GameNumber> = emptyMap(),
    val sequence: ResonanceSequenceState = ResonanceSequenceState(),
    val convergence: ConvergenceState = ConvergenceState()
) {
    init {
        require(chargeByAffinityId.values.all { it >= GameNumber.ZERO }) {
            "Resonance charge cannot be negative"
        }
    }
}
