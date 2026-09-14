package com.idlerpg.game.domain.system.combat

import com.idlerpg.game.core.id.InstanceId
import com.idlerpg.game.core.number.GameNumber
import com.idlerpg.game.domain.engine.EngineContext
import com.idlerpg.game.domain.event.GameEvent
import com.idlerpg.game.domain.model.GameState
import com.idlerpg.game.domain.model.combat.CombatStatus
import com.idlerpg.game.domain.system.world.EncounterSystem

data class DeathResolutionResult(
    val state: GameState,
    val events: List<GameEvent>,
    val encounterCleared: Boolean
)

/** Resolves every newly dead enemy in stable InstanceId order after an action or status tick. */
object DeathResolutionSystem {
    fun resolveEnemyDeaths(
        state: GameState,
        killerInstanceId: InstanceId,
        context: EngineContext
    ): DeathResolutionResult {
        val combat = state.run.combat
        require(combat.status == CombatStatus.ACTIVE) {
            "Death resolution requires ACTIVE combat"
        }
        require(combat.enemies.isNotEmpty()) { "ACTIVE combat requires enemies" }
        require(combat.combatSequenceId > 0L) {
            "ACTIVE combat must have a positive combatSequenceId"
        }
        val deadEnemies = combat.enemies
            .filter { it.combatant.currentHealth == GameNumber.ZERO }
            .sortedBy { it.instanceId }
        if (deadEnemies.isEmpty()) {
            return DeathResolutionResult(state, emptyList(), encounterCleared = false)
        }
        val transition = EncounterSystem.resolveEnemyDeaths(
            state = state,
            deadEnemies = deadEnemies,
            killerInstanceId = killerInstanceId,
            context = context
        )
        return DeathResolutionResult(
            state = transition.state,
            events = transition.events,
            encounterCleared = transition.encounterCleared
        )
    }
}
