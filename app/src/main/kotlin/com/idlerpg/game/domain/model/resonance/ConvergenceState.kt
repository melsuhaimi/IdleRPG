package com.idlerpg.game.domain.model.resonance

import com.idlerpg.game.core.id.ContentId
import com.idlerpg.game.core.number.GameNumber
import com.idlerpg.game.core.time.GameTime

/**
 * Run-local mutable Convergence execution state.
 *
 * [triggerCountById] is run-local aggregate state. [encounterTriggerCountById] is reset
 * whenever a new combat encounter starts. Cooldown deadlines use deterministic GameTime.
 */
data class ConvergenceState(
    val readyAtById: Map<ContentId, GameTime> = emptyMap(),
    val triggerCountById: Map<ContentId, GameNumber> = emptyMap(),
    val encounterTriggerCountById: Map<ContentId, Long> = emptyMap()
) {
    init {
        require(encounterTriggerCountById.values.all { it >= 0L }) {
            "Convergence encounter trigger counts cannot be negative"
        }
    }
}
