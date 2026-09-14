package com.idlerpg.game.domain.system.combat

import com.idlerpg.game.core.id.ContentId
import com.idlerpg.game.core.time.GameDuration
import com.idlerpg.game.domain.model.GameState

/** Deterministic player-skill cooldown calculations using simulation timestamps only. */
object CooldownSystem {

    fun isPlayerSkillReady(
        state: GameState,
        skillId: ContentId
    ): Boolean {
        val player = state.run.combat.playerCombatant ?: return false
        val readyAt = player.cooldowns.readyAtByActionId[skillId] ?: return true
        return readyAt <= state.engine.simulationTime
    }

    fun startPlayerCooldown(
        state: GameState,
        skillId: ContentId,
        cooldown: GameDuration
    ): GameState {
        if (cooldown == GameDuration.ZERO) {
            return state
        }

        val player = state.run.combat.playerCombatant
            ?: error("Cannot start player cooldown without active player combatant")
        val readyAt = state.engine.simulationTime + cooldown
        val updatedDeadlines = player.cooldowns.readyAtByActionId.toMutableMap()
        updatedDeadlines[skillId] = readyAt

        return state.copy(
            run = state.run.copy(
                combat = state.run.combat.copy(
                    playerCombatant = player.copy(
                        cooldowns = player.cooldowns.copy(
                            readyAtByActionId = updatedDeadlines.toMap()
                        )
                    )
                )
            )
        )
    }
}
