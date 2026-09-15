package com.idlerpg.game.domain.system.combat

import com.idlerpg.game.core.id.ContentId
import com.idlerpg.game.core.number.GameNumber
import com.idlerpg.game.data.content.ContentRegistry
import com.idlerpg.game.domain.engine.EngineContext
import com.idlerpg.game.domain.engine.ScheduledAction
import com.idlerpg.game.domain.engine.ScheduledActionExecution
import com.idlerpg.game.domain.engine.ScheduledActionHandler
import com.idlerpg.game.domain.engine.ScheduledActionSource
import com.idlerpg.game.domain.engine.ScheduledActionType
import com.idlerpg.game.domain.event.ActionSelected
import com.idlerpg.game.domain.event.CombatStarted
import com.idlerpg.game.domain.event.GameEvent
import com.idlerpg.game.domain.event.HealingApplied
import com.idlerpg.game.domain.event.ResonanceConsumed
import com.idlerpg.game.domain.event.SkillCastQueueCleared
import com.idlerpg.game.domain.event.SkillCastQueueConsumed
import com.idlerpg.game.domain.event.SkillCastQueueDeferred
import com.idlerpg.game.domain.event.SkillQueueClearReason
import com.idlerpg.game.domain.definition.world.EncounterDefinition
import com.idlerpg.game.domain.model.GameState
import com.idlerpg.game.domain.model.combat.CombatState
import com.idlerpg.game.domain.model.combat.CombatStatus
import com.idlerpg.game.domain.model.combat.CombatantState
import com.idlerpg.game.domain.model.combat.QueuedPlayerAction
import com.idlerpg.game.domain.model.doctrine.DoctrineAction
import com.idlerpg.game.domain.system.doctrine.DoctrineEvaluator
import com.idlerpg.game.domain.system.enemy.EnemyBehaviorSystem
import com.idlerpg.game.domain.system.enemy.EnemyEliteSystem
import com.idlerpg.game.domain.system.enemy.EnemyFactory
import com.idlerpg.game.domain.system.enemy.EnemyScalingSystem
import com.idlerpg.game.domain.system.resonance.ResonanceSystem
import com.idlerpg.game.domain.system.skill.SkillExecutionRejectionDisposition
import com.idlerpg.game.domain.system.skill.SkillExecutionResult
import com.idlerpg.game.domain.system.skill.SkillSystem
import com.idlerpg.game.domain.system.skill.SkillValidationSystem
import com.idlerpg.game.domain.system.stats.DerivedStatSystem
import com.idlerpg.game.domain.system.world.EncounterSystem
import com.idlerpg.game.domain.system.adaptation.MutationSystem

/** Raw state/events produced when EncounterSystem starts combat. */
data class CombatStartResult(
    val state: GameState,
    val events: List<GameEvent>
)

/**
 * High-level deterministic combat coordinator.
 *
 * FBE-01 preserves one authored action stream. At each PLAYER_DECISION an executable
 * queued manual skill has priority; otherwise Doctrine selects the action and retains its
 * existing Basic Attack fallback. A manual tap never inserts an extra action between
 * scheduled decision points.
 */
object CombatSystem : ScheduledActionSource, ScheduledActionHandler {

    fun startCombat(
        state: GameState,
        enemyDefinitionIds: List<ContentId>,
        encounterDefinitionId: ContentId,
        regionDefinitionId: ContentId,
        scalingTier: Long,
        context: EngineContext
    ): CombatStartResult {
        require(enemyDefinitionIds.size in 1..EncounterDefinition.MAX_ACTIVE_ENEMIES) {
            "Combat requires between one and ${EncounterDefinition.MAX_ACTIVE_ENEMIES} active enemies"
        }
        val existingCombat = state.run.combat
        require(existingCombat.status != CombatStatus.ACTIVE) {
            "Cannot start new combat while combat is already ACTIVE"
        }

        val playerInstanceId = context.nextInstanceId()
        val playerCombatant = CombatantState(
            instanceId = playerInstanceId,
            currentHealth = state.run.player.currentHealth
        )
        val eliteTraitIds = context.contentRegistry.encounter(encounterDefinitionId)
            .eliteModifiers.map { it.id }.toSortedSet()
        val enemyCreations = enemyDefinitionIds.map { definitionId ->
            EnemyFactory.create(
                state = state,
                definitionId = definitionId,
                context = context,
                scalingTier = scalingTier,
                regionDefinitionId = regionDefinitionId,
                activeTraitIds = eliteTraitIds
            )
        }
        val enemies = enemyCreations.map { it.enemy }.sortedBy { it.instanceId }
        val nextSequenceId = if (existingCombat.combatSequenceId == 0L) {
            1L
        } else {
            Math.addExact(existingCombat.combatSequenceId, 1L)
        }
        val firstPlayerDecisionAt = state.engine.simulationTime +
            DerivedStatSystem.basicAttackInterval(
                state,
                context.contentRegistry.basicAttack,
                context.contentRegistry
            )
        val firstEnemyDecisions = enemies.mapNotNull { enemy ->
            val definition = context.contentRegistry.enemy(enemy.definitionId)
            val attackId = definition.attackDefinitionId ?: return@mapNotNull null
            val attack = context.contentRegistry.enemyAttack(attackId)
            enemy.instanceId to (
                state.engine.simulationTime + MutationSystem.enemyActionInterval(
                    EnemyEliteSystem.attackInterval(
                        EnemyScalingSystem.scaledAttackInterval(
                            enemyDefinition = definition,
                            attackDefinition = attack,
                            scalingTier = enemy.scalingTier
                        ),
                        enemy
                    ),
                    enemy,
                    context.contentRegistry
                )
            )
        }.toMap()

        val combat = CombatState(
            status = CombatStatus.ACTIVE,
            playerCombatant = playerCombatant,
            enemies = enemies,
            nextPlayerDecisionAt = firstPlayerDecisionAt,
            nextEnemyDecisionAt = firstEnemyDecisions,
            combatSequenceId = nextSequenceId,
            encounterStartedAt = state.engine.simulationTime
        )
        val combatState = ResonanceSystem.beginEncounter(
            state.copy(run = state.run.copy(combat = combat))
        )
        return CombatStartResult(
            state = combatState,
            events = buildList {
                enemyCreations.forEach { addAll(it.events) }
                add(CombatStarted(nextSequenceId, encounterDefinitionId))
            }
        )
    }

    fun startSingleEnemyCombat(
        state: GameState,
        enemyDefinitionId: ContentId,
        encounterDefinitionId: ContentId,
        regionDefinitionId: ContentId,
        scalingTier: Long,
        context: EngineContext
    ): CombatStartResult = startCombat(
        state = state,
        enemyDefinitionIds = listOf(enemyDefinitionId),
        encounterDefinitionId = encounterDefinitionId,
        regionDefinitionId = regionDefinitionId,
        scalingTier = scalingTier,
        context = context
    )

    fun scheduledActions(
        state: GameState,
        contentRegistry: ContentRegistry
    ): List<ScheduledAction> {
        val combat = state.run.combat
        if (combat.status != CombatStatus.ACTIVE) return emptyList()
        val player = combat.playerCombatant ?: return emptyList()
        val livingEnemies = combat.enemies.filter { it.combatant.currentHealth > GameNumber.ZERO }
        if (livingEnemies.isEmpty() || player.currentHealth == GameNumber.ZERO) return emptyList()

        val actions = mutableListOf<ScheduledAction>()
        combat.nextPlayerDecisionAt?.let { dueAt ->
            actions += ScheduledAction(
                dueAt = dueAt,
                type = ScheduledActionType.PLAYER_DECISION,
                stableTieBreakKey = player.instanceId.value,
                ownerInstanceId = player.instanceId
            )
        }
        livingEnemies.forEach { enemy ->
            val definition = contentRegistry.enemy(enemy.definitionId)
            val attackId = definition.attackDefinitionId ?: return@forEach
            val attack = contentRegistry.enemyAttack(attackId)
            val authoredFirst = (combat.encounterStartedAt ?: state.engine.simulationTime) +
                MutationSystem.enemyActionInterval(
                    EnemyEliteSystem.attackInterval(
                        EnemyScalingSystem.scaledAttackInterval(
                            enemyDefinition = definition,
                            attackDefinition = attack,
                            scalingTier = enemy.scalingTier
                        ),
                        enemy
                    ),
                    enemy,
                    contentRegistry
                )
            val dueAt = combat.nextEnemyDecisionAt[enemy.instanceId]
                ?: if (authoredFirst < state.engine.simulationTime) state.engine.simulationTime else authoredFirst
            actions += ScheduledAction(
                dueAt = dueAt,
                type = ScheduledActionType.ENEMY_DECISION,
                stableTieBreakKey = enemy.instanceId.value,
                ownerInstanceId = enemy.instanceId,
                sourceContentId = attack.id
            )
        }
        return actions
    }

    override fun scheduledActions(state: GameState): List<ScheduledAction> =
        error("CombatSystem.scheduledActions(state) requires ContentRegistry; use scheduledActions(state, contentRegistry)")

    override fun execute(
        state: GameState,
        action: ScheduledAction,
        context: EngineContext
    ): ScheduledActionExecution {
        if (action.type == ScheduledActionType.ENEMY_DECISION) {
            return executeEnemyDecision(state, action, context)
        }
        require(action.type == ScheduledActionType.PLAYER_DECISION) {
            "CombatSystem cannot execute scheduled action type ${action.type}"
        }

        val combat = state.run.combat
        require(combat.status == CombatStatus.ACTIVE) {
            "PLAYER_DECISION requires ACTIVE combat"
        }

        val actor = combat.playerCombatant
            ?: error("ACTIVE combat has no playerCombatant")

        require(action.ownerInstanceId == actor.instanceId) {
            "PLAYER_DECISION owner does not match active player combatant"
        }
        require(action.dueAt == state.engine.simulationTime) {
            "Scheduled action must execute at the state's current simulation time"
        }

        val target = TargetingSystem.primaryLivingEnemy(combat)
            ?: error("ACTIVE combat has no living enemy")

        val events = mutableListOf<GameEvent>()
        var decisionState = state
        var selectedRecovery = DerivedStatSystem.basicAttackInterval(
            state = state,
            definition = context.contentRegistry.basicAttack,
            contentRegistry = context.contentRegistry
        )
        var actionResolution: ActionResolutionResult? = null

        val queued = combat.queuedPlayerAction
        if (queued is QueuedPlayerAction.Skill) {
            val rejection = SkillValidationSystem.rejectionReason(
                state = state,
                skillId = queued.skillId,
                contentRegistry = context.contentRegistry
            )

            if (rejection == null) {
                val skill = context.contentRegistry.skill(queued.skillId)
                events += ActionSelected(
                    actorInstanceId = actor.instanceId,
                    actionId = skill.id
                )

                when (val execution = SkillSystem.executePlayerSkill(
                    state = state,
                    actorInstanceId = actor.instanceId,
                    definition = skill,
                    context = context
                )) {
                    is SkillExecutionResult.Accepted -> {
                        selectedRecovery = execution.recovery
                        val consumedState = execution.state.copy(
                            run = execution.state.run.copy(
                                combat = execution.state.run.combat.copy(
                                    queuedPlayerAction = null
                                )
                            )
                        )
                        events += SkillCastQueueConsumed(skill.id)
                        actionResolution = ActionResolutionResult(
                            state = consumedState,
                            events = execution.events
                        )
                    }

                    is SkillExecutionResult.Rejected ->
                        error(
                            "Manual queued skill ${skill.id} passed validation but " +
                                "SkillSystem rejected it: ${execution.reason}"
                        )
                }
            } else {
                when (SkillValidationSystem.executionRejectionDisposition(rejection)) {
                    SkillExecutionRejectionDisposition.TRANSIENT ->
                        events += SkillCastQueueDeferred(
                            skillId = queued.skillId,
                            reasonCode = rejection.code
                        )

                    SkillExecutionRejectionDisposition.PERMANENT -> {
                        decisionState = state.copy(
                            run = state.run.copy(
                                combat = state.run.combat.copy(
                                    queuedPlayerAction = null
                                )
                            )
                        )
                        events += SkillCastQueueCleared(
                            skillId = queued.skillId,
                            reason = SkillQueueClearReason.INVALIDATED
                        )
                    }
                }
            }
        }

        if (actionResolution == null) {
            val doctrine = DoctrineEvaluator.selectAction(
                state = decisionState,
                context = context
            )
            events += doctrine.events

            actionResolution =
                when (val selected = doctrine.action) {
                    DoctrineAction.UseBasicAttack -> {
                        val basicAttack = context.contentRegistry.basicAttack
                        events += ActionSelected(
                            actorInstanceId = actor.instanceId,
                            actionId = basicAttack.id
                        )
                        ActionResolutionSystem.resolveBasicAttack(
                            state = decisionState,
                            actorInstanceId = actor.instanceId,
                            targetInstanceId = target.instanceId,
                            definition = basicAttack,
                            context = context
                        )
                    }

                    is DoctrineAction.UseSkill -> {
                        val skill = context.contentRegistry.skill(selected.skillId)
                        events += ActionSelected(
                            actorInstanceId = actor.instanceId,
                            actionId = skill.id
                        )
                        when (val execution = SkillSystem.executePlayerSkill(
                            state = decisionState,
                            actorInstanceId = actor.instanceId,
                            definition = skill,
                            context = context
                        )) {
                            is SkillExecutionResult.Accepted -> {
                                selectedRecovery = execution.recovery
                                ActionResolutionResult(execution.state, execution.events)
                            }

                            is SkillExecutionResult.Rejected ->
                                error(
                                    "Doctrine selected skill ${skill.id} that " +
                                        "SkillSystem rejected: ${execution.reason}"
                                )
                        }
                    }
                }
        }

        events += actionResolution.events

        val deathResolution = DeathResolutionSystem.resolveEnemyDeaths(
            state = actionResolution.state,
            killerInstanceId = actor.instanceId,
            context = context
        )
        events += deathResolution.events

        val resolvedCombat = deathResolution.state.run.combat
        var finalState =
            if (resolvedCombat.status == CombatStatus.ACTIVE &&
                !deathResolution.encounterCleared
            ) {
                val nextDecisionAt = state.engine.simulationTime + selectedRecovery
                deathResolution.state.copy(
                    run = deathResolution.state.run.copy(
                        combat = resolvedCombat.copy(
                            nextPlayerDecisionAt = nextDecisionAt
                        )
                    )
                )
            } else {
                deathResolution.state
            }

        if (finalState.run.combat.status != CombatStatus.ACTIVE) {
            val remainingQueue = finalState.run.combat.queuedPlayerAction
            if (remainingQueue is QueuedPlayerAction.Skill) {
                finalState = finalState.copy(
                    run = finalState.run.copy(
                        combat = finalState.run.combat.copy(
                            queuedPlayerAction = null
                        )
                    )
                )
                events += SkillCastQueueCleared(
                    skillId = remainingQueue.skillId,
                    reason = SkillQueueClearReason.COMBAT_ENDED
                )
            }
        }

        return ScheduledActionExecution(
            state = finalState,
            events = events
        )
    }
    private fun executeEnemyDecision(
        state: GameState,
        action: ScheduledAction,
        context: EngineContext
    ): ScheduledActionExecution {
        val combat = state.run.combat
        require(combat.status == CombatStatus.ACTIVE) { "ENEMY_DECISION requires ACTIVE combat" }
        require(action.dueAt == state.engine.simulationTime) { "Enemy decision must execute at current simulation time" }
        val enemy = combat.enemies.firstOrNull { it.instanceId == action.ownerInstanceId }
            ?: error("ENEMY_DECISION owner is not an active enemy")
        require(enemy.combatant.currentHealth > GameNumber.ZERO) { "Dead enemy cannot act" }
        val enemyDefinition = context.contentRegistry.enemy(enemy.definitionId)
        val attackId = EnemyBehaviorSystem.selectAttackId(enemyDefinition)
            ?: error("Enemy ${enemy.definitionId} has no authored attack")
        require(action.sourceContentId == attackId) { "Scheduled enemy attack does not match authored attack" }
        val attack = context.contentRegistry.enemyAttack(attackId)
        val player = combat.playerCombatant ?: error("ACTIVE combat has no player")

        val events = mutableListOf<GameEvent>()
        events += ActionSelected(enemy.instanceId, attack.id)
        var actingEnemy = enemy
        var actionState = state
        val region = state.run.world.activeRegionId?.let(context.contentRegistry::regionOrNull)
        val maximumHealth = EnemyEliteSystem.spawnHealth(
            EnemyScalingSystem.scaledHealth(enemyDefinition, region, enemy.scalingTier),
            enemy.activeTraitIds
        )
        val regeneration = EnemyEliteSystem.regenerationAmount(maximumHealth, enemy)
        if (regeneration > GameNumber.ZERO && enemy.combatant.currentHealth < maximumHealth) {
            val missing = maximumHealth - enemy.combatant.currentHealth
            val applied = if (regeneration > missing) missing else regeneration
            actingEnemy = enemy.copy(
                combatant = enemy.combatant.copy(
                    currentHealth = enemy.combatant.currentHealth + applied
                )
            )
            actionState = state.copy(
                run = state.run.copy(
                    combat = state.run.combat.copy(
                        enemies = state.run.combat.enemies.map {
                            if (it.instanceId == enemy.instanceId) actingEnemy else it
                        }
                    )
                )
            )
            events += HealingApplied(enemy.instanceId, enemy.instanceId, applied)
        }
        val scaledDamage = EnemyScalingSystem.scaledAttack(
            enemyDefinition = enemyDefinition,
            attackDefinition = attack,
            scalingTier = actingEnemy.scalingTier
        )
        val eliteDamage = EnemyEliteSystem.attackDamage(scaledDamage, actingEnemy)
        val mutationAdjustedDamage = MutationSystem.enemyOutgoingDamage(
            state = actionState,
            carrier = actingEnemy,
            baseDamage = eliteDamage,
            contentRegistry = context.contentRegistry
        )
        val damage = DamageSystem.dealToPlayer(
            state = actionState,
            sourceInstanceId = enemy.instanceId,
            baseDamage = mutationAdjustedDamage,
            armorPenetration = attack.armorPenetration,
            damageKindId = attack.damageKind.id,
            contentRegistry = context.contentRegistry
        )
        var nextState = damage.state
        events += damage.event

        if (nextState.run.combat.playerCombatant?.currentHealth != GameNumber.ZERO) {
            attack.appliedStatusId?.let { statusId ->
                val status = StatusEffectSystem.apply(
                    state = nextState,
                    sourceInstanceId = enemy.instanceId,
                    targetInstanceId = player.instanceId,
                    definition = context.contentRegistry.status(statusId),
                    context = context
                )
                nextState = status.state
                events += status.events
            }
            val resonanceDrain = attack.resonanceDrain +
                MutationSystem.additionalResonanceDrain(actingEnemy, context.contentRegistry)
            if (resonanceDrain > GameNumber.ZERO) {
                val charged = nextState.run.resonance.chargeByAffinityId.entries
                    .filter { it.value > GameNumber.ZERO }
                    .sortedWith(
                        compareByDescending<Map.Entry<ContentId, GameNumber>> { it.value }
                            .thenBy { it.key }
                    )
                    .firstOrNull()
                if (charged != null) {
                    val drained = if (charged.value < resonanceDrain) {
                        charged.value
                    } else {
                        resonanceDrain
                    }
                    nextState = nextState.copy(
                        run = nextState.run.copy(
                            resonance = nextState.run.resonance.copy(
                                chargeByAffinityId = nextState.run.resonance.chargeByAffinityId +
                                    (charged.key to (charged.value - drained))
                            )
                        )
                    )
                    events += ResonanceConsumed(charged.key, drained)
                }
            }
        }

        if (nextState.run.combat.playerCombatant?.currentHealth == GameNumber.ZERO) {
            val defeat = EncounterSystem.resolvePlayerDefeat(nextState)
            nextState = defeat.state
            events += defeat.events
        } else {
            nextState = nextState.copy(
                run = nextState.run.copy(
                    combat = nextState.run.combat.copy(
                        nextEnemyDecisionAt = nextState.run.combat.nextEnemyDecisionAt +
                            (enemy.instanceId to (
                                state.engine.simulationTime +
                                    MutationSystem.enemyActionInterval(
                                        EnemyEliteSystem.attackInterval(
                                            EnemyScalingSystem.scaledAttackInterval(
                                                enemyDefinition = enemyDefinition,
                                                attackDefinition = attack,
                                                scalingTier = actingEnemy.scalingTier
                                            ),
                                            actingEnemy
                                        ),
                                        actingEnemy,
                                        context.contentRegistry
                                    )
                            ))
                    )
                )
            )
        }
        return ScheduledActionExecution(nextState, events)
    }

}
