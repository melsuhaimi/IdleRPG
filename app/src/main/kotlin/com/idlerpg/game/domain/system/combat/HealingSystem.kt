package com.idlerpg.game.domain.system.combat

import com.idlerpg.game.core.id.InstanceId
import com.idlerpg.game.core.number.GameNumber
import com.idlerpg.game.core.number.GameMath
import com.idlerpg.game.domain.event.HealingApplied
import com.idlerpg.game.domain.model.GameState
import com.idlerpg.game.domain.system.stats.DerivedStatSystem
import com.idlerpg.game.data.content.ContentRegistry
import com.idlerpg.game.domain.system.adaptation.MutationSystem

/** Result of one deterministic healing primitive. */
data class HealingResult(
    val state: GameState,
    val event: HealingApplied
)

/** Owns player healing and the no-overheal clamp policy. */
object HealingSystem {

    fun healPlayer(
        state: GameState,
        sourceInstanceId: InstanceId?,
        targetInstanceId: InstanceId,
        requestedAmount: GameNumber,
        contentRegistry: ContentRegistry,
        scaleWithHealingPower: Boolean = true
    ): HealingResult {
        val combatant = state.run.combat.playerCombatant
            ?: error("Cannot heal player without active player combatant")
        require(combatant.instanceId == targetInstanceId) {
            "Healing target $targetInstanceId is not the active player combatant"
        }

        val scaledRequest = if (scaleWithHealingPower) {
            GameMath.applyRatio(requestedAmount, DerivedStatSystem.healingPower(state, contentRegistry))
        } else requestedAmount
        val mutationAdjustedRequest = GameMath.applyRatio(
            scaledRequest,
            MutationSystem.playerHealingMultiplier(state, contentRegistry)
        )
        val maximum = DerivedStatSystem.maximumHealth(state, contentRegistry)
        val current = combatant.currentHealth
        val room = if (current < maximum) maximum - current else GameNumber.ZERO
        val actual = if (mutationAdjustedRequest <= room) mutationAdjustedRequest else room
        val healed = current + actual

        val newCombatant = combatant.copy(currentHealth = healed)
        val newState = state.copy(
            run = state.run.copy(
                player = state.run.player.copy(currentHealth = healed),
                combat = state.run.combat.copy(playerCombatant = newCombatant)
            )
        )

        return HealingResult(
            state = newState,
            event = HealingApplied(
                sourceInstanceId = sourceInstanceId,
                targetInstanceId = targetInstanceId,
                amount = actual
            )
        )
    }
}
