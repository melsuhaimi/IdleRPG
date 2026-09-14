package com.idlerpg.game.domain.model.resonance

import com.idlerpg.game.core.id.ContentId
import com.idlerpg.game.core.id.InstanceId
import com.idlerpg.game.core.number.GameNumber
import com.idlerpg.game.core.time.GameTime
import com.idlerpg.game.domain.definition.Affinity

/** One resolved Resonance emission occurrence produced by an action. */
data class ResonancePulse(
    val affinity: Affinity,
    val amount: GameNumber,
    val sourceDefinitionId: ContentId,
    val sourceInstanceId: InstanceId? = null,
    val emittedAt: GameTime
) {
    init {
        require(amount > GameNumber.ZERO) {
            "ResonancePulse.amount must be > 0"
        }
    }
}
