package com.idlerpg.game.domain.model.combat

import com.idlerpg.game.core.id.ContentId
import com.idlerpg.game.core.id.InstanceId
import com.idlerpg.game.core.number.Ratio
import com.idlerpg.game.core.time.GameTime

/** Structural runtime state for one active status-effect instance. */
data class StatusEffectState(
    val instanceId: InstanceId,
    val definitionId: ContentId,
    val sourceInstanceId: InstanceId? = null,
    val stackCount: Int = 1,
    val potency: Ratio = Ratio.ONE,
    val appliedAt: GameTime,
    val expiresAt: GameTime,
    val nextPeriodicTickAt: GameTime? = null
) {
    init {
        require(stackCount > 0) { "stackCount must be positive: $stackCount" }
        require(expiresAt >= appliedAt) {
            "expiresAt cannot precede appliedAt"
        }
        if (nextPeriodicTickAt != null) {
            require(nextPeriodicTickAt >= appliedAt) {
                "nextPeriodicTickAt cannot precede appliedAt"
            }
            require(nextPeriodicTickAt <= expiresAt) {
                "nextPeriodicTickAt cannot exceed expiresAt"
            }
        }
    }
}
