package com.idlerpg.game.domain.model.combat

import com.idlerpg.game.core.id.InstanceId
import com.idlerpg.game.core.number.GameNumber

/** Common mutable runtime properties for one combatant instance. */
data class CombatantState(
    val instanceId: InstanceId,
    val currentHealth: GameNumber,
    val cooldowns: CooldownState = CooldownState(),
    val statusEffects: List<StatusEffectState> = emptyList()
) {
    init {
        require(statusEffects.map { it.instanceId }.size == statusEffects.map { it.instanceId }.toSet().size) {
            "statusEffects must have unique instance IDs"
        }
    }
}
