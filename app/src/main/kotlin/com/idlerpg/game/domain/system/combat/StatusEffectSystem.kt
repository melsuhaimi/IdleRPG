package com.idlerpg.game.domain.system.combat

import com.idlerpg.game.core.id.InstanceId
import com.idlerpg.game.core.time.GameTime
import com.idlerpg.game.data.content.ContentRegistry
import com.idlerpg.game.domain.definition.combat.StatusEffectDefinition
import com.idlerpg.game.domain.definition.combat.StatusStackingPolicy
import com.idlerpg.game.domain.engine.EngineContext
import com.idlerpg.game.domain.engine.ScheduledAction
import com.idlerpg.game.domain.engine.ScheduledActionExecution
import com.idlerpg.game.domain.engine.ScheduledActionHandler
import com.idlerpg.game.domain.engine.ScheduledActionSource
import com.idlerpg.game.domain.engine.ScheduledActionType
import com.idlerpg.game.domain.event.GameEvent
import com.idlerpg.game.domain.event.StatusApplied
import com.idlerpg.game.domain.event.StatusExpired
import com.idlerpg.game.domain.model.GameState
import com.idlerpg.game.domain.model.combat.CombatStatus
import com.idlerpg.game.domain.model.combat.CombatantState
import com.idlerpg.game.domain.model.combat.StatusEffectState

/** State/events produced by status application/removal. */
data class StatusTransitionResult(
    val state: GameState,
    val events: List<GameEvent>
)

/**
 * Owns deterministic status application, stacking, periodic ticks, expiration and removal.
 *
 * Scheduled actions are projected entirely from canonical StatusEffectState. No separate
 * mutable timer queue or wall-clock read is used.
 */
object StatusEffectSystem : ScheduledActionSource, ScheduledActionHandler {

    fun apply(
        state: GameState,
        sourceInstanceId: InstanceId?,
        targetInstanceId: InstanceId,
        definition: StatusEffectDefinition,
        context: EngineContext
    ): StatusTransitionResult {
        val target = findCombatant(state, targetInstanceId)
            ?: error("Status target $targetInstanceId is not in active combat")
        val existing = target.statusEffects
            .firstOrNull { it.definitionId == definition.id }

        val appliedAt = state.engine.simulationTime
        val expiresAt = appliedAt + definition.duration
        val nextTickAt = definition.periodicInterval?.let { appliedAt + it }

        val status = if (existing == null) {
            StatusEffectState(
                instanceId = context.nextInstanceId(),
                definitionId = definition.id,
                sourceInstanceId = sourceInstanceId,
                stackCount = 1,
                appliedAt = appliedAt,
                expiresAt = expiresAt,
                nextPeriodicTickAt = nextTickAt
            )
        } else {
            val newStacks = when (definition.stackingPolicy) {
                StatusStackingPolicy.REFRESH_DURATION -> 1
                StatusStackingPolicy.STACK_AND_REFRESH ->
                    minOf(existing.stackCount + 1, definition.maximumStacks)
            }
            existing.copy(
                sourceInstanceId = sourceInstanceId ?: existing.sourceInstanceId,
                stackCount = newStacks,
                appliedAt = appliedAt,
                expiresAt = expiresAt,
                nextPeriodicTickAt = nextTickAt
            )
        }

        val updated = target.copy(
            statusEffects = target.statusEffects
                .filterNot { it.definitionId == definition.id } + status
        )
        val newState = replaceCombatant(state, updated)

        return StatusTransitionResult(
            state = newState,
            events = listOf(
                StatusApplied(
                    statusDefinitionId = definition.id,
                    statusInstanceId = status.instanceId,
                    sourceInstanceId = sourceInstanceId,
                    targetInstanceId = targetInstanceId
                )
            )
        )
    }

    fun removeByDefinition(
        state: GameState,
        targetInstanceId: InstanceId,
        statusDefinitionId: com.idlerpg.game.core.id.ContentId
    ): StatusTransitionResult {
        val target = findCombatant(state, targetInstanceId)
            ?: error("Status target $targetInstanceId is not in active combat")
        val removed = target.statusEffects
            .filter { it.definitionId == statusDefinitionId }
            .sortedBy { it.instanceId }
        if (removed.isEmpty()) {
            return StatusTransitionResult(state, emptyList())
        }

        val updated = target.copy(
            statusEffects = target.statusEffects
                .filterNot { it.definitionId == statusDefinitionId }
        )
        return StatusTransitionResult(
            state = replaceCombatant(state, updated),
            events = removed.map { status ->
                StatusExpired(
                    statusDefinitionId = status.definitionId,
                    statusInstanceId = status.instanceId,
                    targetInstanceId = targetInstanceId
                )
            }
        )
    }

    override fun scheduledActions(state: GameState): List<ScheduledAction> {
        if (state.run.combat.status != CombatStatus.ACTIVE) {
            return emptyList()
        }

        return buildList {
            state.run.combat.playerCombatant?.let { combatant ->
                addAll(actionsFor(combatant))
            }
            state.run.combat.enemies
                .sortedBy { it.instanceId }
                .forEach { enemy -> addAll(actionsFor(enemy.combatant)) }
        }
    }

    private fun actionsFor(combatant: CombatantState): List<ScheduledAction> =
        combatant.statusEffects
            .sortedBy { it.instanceId }
            .map { status ->
                val next = earlier(status.nextPeriodicTickAt, status.expiresAt)
                ScheduledAction(
                    dueAt = next,
                    type = ScheduledActionType.STATUS_PERIODIC,
                    stableTieBreakKey = status.instanceId.value,
                    ownerInstanceId = combatant.instanceId,
                    sourceContentId = status.definitionId
                )
            }

    override fun execute(
        state: GameState,
        action: ScheduledAction,
        context: EngineContext
    ): ScheduledActionExecution {
        require(action.type == ScheduledActionType.STATUS_PERIODIC) {
            "StatusEffectSystem cannot execute ${action.type}"
        }
        require(action.dueAt == state.engine.simulationTime) {
            "Status action must execute at current simulation time"
        }

        val targetId = action.ownerInstanceId
            ?: error("STATUS_PERIODIC requires ownerInstanceId")
        val statusId = InstanceId(action.stableTieBreakKey)
        val target = findCombatant(state, targetId)
            ?: error("Scheduled status target $targetId is not present")
        val status = target.statusEffects
            .firstOrNull { it.instanceId == statusId }
            ?: error("Scheduled status instance $statusId is not present")
        require(action.sourceContentId == status.definitionId) {
            "Scheduled status definition does not match canonical status state"
        }

        val definition = context.contentRegistry.status(status.definitionId)
        var currentState = state
        val events = mutableListOf<GameEvent>()
        val tickDue = status.nextPeriodicTickAt == state.engine.simulationTime

        if (tickDue) {
            for (stackIndex in 0 until status.stackCount) {
                val currentTarget = findCombatant(currentState, targetId) ?: break
                if (currentTarget.currentHealth == com.idlerpg.game.core.number.GameNumber.ZERO) break
                val tick = ActionResolutionSystem.resolvePrimitiveEffects(
                    state = currentState,
                    actorInstanceId = status.sourceInstanceId
                        ?: currentState.run.combat.playerCombatant?.instanceId
                        ?: targetId,
                    targetInstanceId = targetId,
                    effects = definition.periodicEffects,
                    adaptationAffinitySequence = definition.affinityTags.sortedBy { it.id },
                    context = context
                )
                currentState = tick.state
                events += tick.events
            }
        }

        val refreshedTarget = findCombatant(currentState, targetId)
        val refreshedStatus = refreshedTarget?.statusEffects
            ?.firstOrNull { it.instanceId == statusId }

        if (refreshedStatus != null &&
            refreshedStatus.expiresAt <= currentState.engine.simulationTime
        ) {
            val removed = removeExact(currentState, targetId, statusId)
            currentState = removed.state
            events += removed.events

            if (definition.expireEffects.isNotEmpty()) {
                val expired = ActionResolutionSystem.resolveFollowUpEffects(
                    state = currentState,
                    actorInstanceId = refreshedStatus.sourceInstanceId
                        ?: currentState.run.combat.playerCombatant?.instanceId
                        ?: targetId,
                    targetInstanceId = targetId,
                    effects = definition.expireEffects,
                    adaptationAffinitySequence = definition.affinityTags.sortedBy { it.id },
                    context = context
                )
                currentState = expired.state
                events += expired.events
            }
        } else if (tickDue && refreshedStatus != null) {
            val nextTick = definition.periodicInterval?.let { interval ->
                val candidate = currentState.engine.simulationTime + interval
                if (candidate <= refreshedStatus.expiresAt) candidate else null
            }
            val updated = refreshedStatus.copy(nextPeriodicTickAt = nextTick)
            currentState = replaceCombatant(
                currentState,
                refreshedTarget.copy(
                    statusEffects = refreshedTarget.statusEffects.map {
                        if (it.instanceId == statusId) updated else it
                    }
                )
            )
        }

        val enemyTarget = currentState.run.combat.enemies
            .firstOrNull { it.instanceId == targetId }
        if (enemyTarget != null &&
            enemyTarget.combatant.currentHealth == com.idlerpg.game.core.number.GameNumber.ZERO
        ) {
            val killer = status.sourceInstanceId
                ?: currentState.run.combat.playerCombatant?.instanceId
                ?: targetId
            val death = DeathResolutionSystem.resolveEnemyDeaths(
                state = currentState,
                killerInstanceId = killer,
                context = context
            )
            currentState = death.state
            events += death.events
        }

        return ScheduledActionExecution(
            state = currentState,
            events = events
        )
    }

    private fun removeExact(
        state: GameState,
        targetId: InstanceId,
        statusId: InstanceId
    ): StatusTransitionResult {
        val target = findCombatant(state, targetId)
            ?: return StatusTransitionResult(state, emptyList())
        val status = target.statusEffects.firstOrNull { it.instanceId == statusId }
            ?: return StatusTransitionResult(state, emptyList())
        val updated = target.copy(
            statusEffects = target.statusEffects.filterNot { it.instanceId == statusId }
        )
        return StatusTransitionResult(
            state = replaceCombatant(state, updated),
            events = listOf(
                StatusExpired(
                    statusDefinitionId = status.definitionId,
                    statusInstanceId = status.instanceId,
                    targetInstanceId = targetId
                )
            )
        )
    }

    private fun findCombatant(
        state: GameState,
        instanceId: InstanceId
    ): CombatantState? {
        val player = state.run.combat.playerCombatant
        if (player?.instanceId == instanceId) {
            return player
        }
        return state.run.combat.enemies
            .firstOrNull { it.instanceId == instanceId }
            ?.combatant
    }

    private fun replaceCombatant(
        state: GameState,
        combatant: CombatantState
    ): GameState {
        val player = state.run.combat.playerCombatant
        if (player?.instanceId == combatant.instanceId) {
            return state.copy(
                run = state.run.copy(
                    player = state.run.player.copy(
                        currentHealth = combatant.currentHealth
                    ),
                    combat = state.run.combat.copy(
                        playerCombatant = combatant
                    )
                )
            )
        }

        val updatedEnemies = state.run.combat.enemies.map { enemy ->
            if (enemy.instanceId == combatant.instanceId) {
                enemy.copy(combatant = combatant)
            } else {
                enemy
            }
        }
        require(updatedEnemies.any { it.instanceId == combatant.instanceId }) {
            "Combatant ${combatant.instanceId} is not present in active combat"
        }
        return state.copy(
            run = state.run.copy(
                combat = state.run.combat.copy(enemies = updatedEnemies)
            )
        )
    }

    private fun earlier(
        periodic: GameTime?,
        expiresAt: GameTime
    ): GameTime =
        if (periodic != null && periodic < expiresAt) periodic else expiresAt
}
