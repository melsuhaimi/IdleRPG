package com.idlerpg.game.domain.definition.combat

import com.idlerpg.game.core.id.ContentId
import com.idlerpg.game.core.time.GameDuration
import com.idlerpg.game.domain.definition.Affinity

/** Authored definition for the automatic basic attack. */
data class BasicAttackDefinition(
    val id: ContentId,
    val interval: GameDuration,
    val effects: List<EffectSpec>,
    val affinityTags: Set<Affinity> = emptySet()
) {
    init {
        require(interval > GameDuration.ZERO) {
            "BasicAttackDefinition.interval must be > 0: $interval"
        }
        require(effects.isNotEmpty()) {
            "BasicAttackDefinition.effects cannot be empty"
        }
    }
}
