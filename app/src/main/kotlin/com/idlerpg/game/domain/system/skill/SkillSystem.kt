package com.idlerpg.game.domain.system.skill

import com.idlerpg.game.core.id.InstanceId
import com.idlerpg.game.core.time.GameDuration
import com.idlerpg.game.domain.command.CommandRejectionReason
import com.idlerpg.game.domain.definition.combat.SkillDefinition
import com.idlerpg.game.domain.definition.combat.SkillTargetingRule
import com.idlerpg.game.domain.engine.EngineContext
import com.idlerpg.game.domain.event.GameEvent
import com.idlerpg.game.domain.model.GameState
import com.idlerpg.game.domain.system.combat.ActionResolutionSystem
import com.idlerpg.game.domain.system.combat.CooldownSystem
import com.idlerpg.game.domain.system.combat.TargetingSystem
import com.idlerpg.game.domain.system.stats.DerivedStatSystem
import com.idlerpg.game.domain.definition.enemy.EnemyRole

/** Complete result of attempting one already-selected player skill. */
sealed interface SkillExecutionResult {
    data class Accepted(
        val state: GameState,
        val events: List<GameEvent>,
        val recovery: GameDuration
    ) : SkillExecutionResult

    data class Rejected(
        val reason: CommandRejectionReason
    ) : SkillExecutionResult
}

/**
 * Owns skill-level reservation and execution ordering.
 *
 * Costs/cooldown are reserved before primitive effects. Doctrine still owns which action
 * is selected; this system owns whether/how that selected skill executes.
 */
object SkillSystem {

    fun executePlayerSkill(
        state: GameState,
        actorInstanceId: InstanceId,
        definition: SkillDefinition,
        context: EngineContext
    ): SkillExecutionResult {
        val effectiveDefinition = SkillEvolutionSystem.effectiveDefinition(
            state,
            definition,
            context.contentRegistry
        )
        val rejection = SkillValidationSystem.rejectionReason(state, effectiveDefinition)
        if (rejection != null) {
            return SkillExecutionResult.Rejected(rejection)
        }

        val playerCombatant = state.run.combat.playerCombatant
            ?: error("Validated skill execution has no player combatant")
        require(playerCombatant.instanceId == actorInstanceId) {
            "Skill actor does not match active player combatant"
        }

        val targetInstanceId = when (effectiveDefinition.targetingRule) {
            SkillTargetingRule.PRIMARY_ENEMY ->
                TargetingSystem.primaryLivingEnemy(state.run.combat)
                    ?.instanceId
                    ?: error("Validated enemy-target skill has no living enemy")

            SkillTargetingRule.LOWEST_HEALTH_ENEMY ->
                TargetingSystem.lowestHealthEnemy(state.run.combat)
                    ?.instanceId
                    ?: error("Validated enemy-target skill has no living enemy")

            SkillTargetingRule.HIGHEST_HEALTH_ENEMY ->
                TargetingSystem.highestHealthEnemy(state.run.combat)
                    ?.instanceId
                    ?: error("Validated enemy-target skill has no living enemy")

            SkillTargetingRule.PROTECTOR_FIRST ->
                (TargetingSystem.firstWithRole(
                    state.run.combat,
                    context.contentRegistry,
                    setOf(EnemyRole.PROTECTOR)
                ) ?: TargetingSystem.primaryLivingEnemy(state.run.combat))
                    ?.instanceId
                    ?: error("Validated enemy-target skill has no living enemy")

            SkillTargetingRule.CASTER_OR_SUPPORT_FIRST ->
                (TargetingSystem.firstWithRole(
                    state.run.combat,
                    context.contentRegistry,
                    setOf(
                        EnemyRole.CASTER,
                        EnemyRole.DISRUPTOR,
                        EnemyRole.CONTROLLER,
                        EnemyRole.PARASITE
                    )
                ) ?: TargetingSystem.primaryLivingEnemy(state.run.combat))
                    ?.instanceId
                    ?: error("Validated enemy-target skill has no living enemy")

            SkillTargetingRule.SELF -> actorInstanceId
        }

        var reservedState = spendResources(state, effectiveDefinition)
        reservedState = CooldownSystem.startPlayerCooldown(
            state = reservedState,
            skillId = effectiveDefinition.id,
            cooldown = effectiveDefinition.cooldown
        )

        val resolution = ActionResolutionSystem.resolveSkill(
            state = reservedState,
            actorInstanceId = actorInstanceId,
            targetInstanceId = targetInstanceId,
            definition = effectiveDefinition,
            context = context
        )

        return SkillExecutionResult.Accepted(
            state = resolution.state,
            events = resolution.events,
            recovery = DerivedStatSystem.actionInterval(
                state,
                effectiveDefinition.recovery,
                context.contentRegistry
            )
        )
    }

    private fun spendResources(
        state: GameState,
        definition: SkillDefinition
    ): GameState {
        if (definition.resourceCosts.isEmpty()) {
            return state
        }

        val amounts = state.run.player.resources.amounts.toMutableMap()
        for ((resourceId, cost) in definition.resourceCosts.entries.sortedBy { it.key }) {
            val current = amounts[resourceId]
                ?: error("Validated resource cost has no available resource $resourceId")
            amounts[resourceId] = current - cost
        }

        return state.copy(
            run = state.run.copy(
                player = state.run.player.copy(
                    resources = state.run.player.resources.copy(
                        amounts = amounts.toMap()
                    )
                )
            )
        )
    }
}
