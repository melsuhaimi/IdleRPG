package com.idlerpg.game.domain.system.world

import com.idlerpg.game.core.config.BalanceConfig
import com.idlerpg.game.core.id.ContentId
import com.idlerpg.game.core.id.InstanceId
import com.idlerpg.game.core.number.GameNumber
import com.idlerpg.game.data.content.ContentRegistry
import com.idlerpg.game.domain.engine.EngineContext
import com.idlerpg.game.domain.engine.ScheduledAction
import com.idlerpg.game.domain.engine.ScheduledActionExecution
import com.idlerpg.game.domain.engine.ScheduledActionHandler
import com.idlerpg.game.domain.engine.ScheduledActionType
import com.idlerpg.game.domain.event.CombatEndReason
import com.idlerpg.game.domain.event.CombatEnded
import com.idlerpg.game.domain.event.EncounterCleared
import com.idlerpg.game.domain.event.EncounterFailed
import com.idlerpg.game.domain.event.EncounterRetreated
import com.idlerpg.game.domain.event.EncounterStarted
import com.idlerpg.game.domain.event.EncounterWaveStarted
import com.idlerpg.game.domain.event.EnemyKilled
import com.idlerpg.game.domain.event.GameEvent
import com.idlerpg.game.domain.event.BossPhaseChanged
import com.idlerpg.game.domain.event.InventoryProgressionBlocked
import com.idlerpg.game.domain.event.PlayerDefeated
import com.idlerpg.game.domain.event.SkillCastQueueCleared
import com.idlerpg.game.domain.event.SkillQueueClearReason
import com.idlerpg.game.domain.model.GameState
import com.idlerpg.game.domain.model.combat.CombatState
import com.idlerpg.game.domain.model.combat.CombatStatus
import com.idlerpg.game.domain.model.combat.EnemyState
import com.idlerpg.game.domain.model.combat.QueuedPlayerAction
import com.idlerpg.game.domain.model.world.EncounterState
import com.idlerpg.game.domain.model.world.EncounterStatus
import com.idlerpg.game.domain.system.adaptation.AdaptationExposureSystem
import com.idlerpg.game.domain.system.adaptation.AdaptationSystem
import com.idlerpg.game.domain.system.adaptation.MutationSystem
import com.idlerpg.game.domain.system.combat.CombatSystem
import com.idlerpg.game.domain.system.enemy.EnemyScalingSystem
import com.idlerpg.game.domain.system.enemy.EnemyFactory
import com.idlerpg.game.domain.system.reward.RewardGrantResult
import com.idlerpg.game.domain.system.inventory.InventoryCapacitySystem
import com.idlerpg.game.domain.system.reward.RewardSystem

/** State/events from starting or continuing one explicit encounter. */
data class EncounterTransitionResult(
    val state: GameState,
    val events: List<GameEvent>
)

/** State/events from resolving the terminal kill of the current encounter. */
data class EncounterVictoryResult(
    val state: GameState,
    val events: List<GameEvent>
)

data class EncounterDeathTransition(
    val state: GameState,
    val events: List<GameEvent>,
    val encounterCleared: Boolean
)

/**
 * Explicit encounter lifecycle owner through Foundation 11.
 *
 * A completed encounter is first committed as CLEARED. If its definition points to a
 * next encounter, WorldSystem projects an ENCOUNTER_LIFECYCLE action at the same
 * simulation timestamp. This keeps continuation event-driven and canonical.
 */
object EncounterSystem : ScheduledActionHandler {

    fun startEncounter(
        state: GameState,
        encounterDefinitionId: ContentId,
        context: EngineContext
    ): EncounterTransitionResult {
        val regionId = state.run.world.activeRegionId
            ?: error("Cannot start encounter without an active region")
        val encounterDefinition =
            context.contentRegistry.encounter(encounterDefinitionId)

        require(encounterDefinition.regionId == regionId) {
            "Encounter $encounterDefinitionId does not belong to active region $regionId"
        }
        val firstWaveEnemyIds = encounterDefinition.enemyDefinitionIdsForWave(1)
        require(firstWaveEnemyIds.size in 1..5) {
            "An encounter wave must contain between one and five active enemies"
        }
        require(encounterDefinition.waves in 1..10) {
            "An encounter must contain between one and ten waves"
        }

        val currentEncounter = state.run.world.currentEncounter
        require(currentEncounter?.status != EncounterStatus.ACTIVE) {
            "Cannot start a new encounter while another encounter is ACTIVE"
        }
        require(state.run.combat.status != CombatStatus.ACTIVE) {
            "Cannot start a new encounter while combat is ACTIVE"
        }

        val preparedState = if (currentEncounter?.status == EncounterStatus.FAILED ||
            state.run.combat.status == CombatStatus.DEFEAT || state.run.player.currentHealth == GameNumber.ZERO
        ) {
            state.copy(
                run = state.run.copy(
                    player = state.run.player.copy(
                        currentHealth = com.idlerpg.game.domain.system.stats.DerivedStatSystem
                            .maximumHealth(state, context.contentRegistry)
                    )
                )
            )
        } else state

        val regionDefinition = context.contentRegistry.region(regionId)
        val encounterIndex = regionDefinition.encounterIds.indexOf(encounterDefinition.id)
            .takeIf { it >= 0 }
            ?.plus(1)
            ?.toLong()
            ?: error("Encounter ${encounterDefinition.id} is not ordered in region $regionId")
        val encounterSeed = context.random.nextLong(Long.MAX_VALUE)
        val scalingTier =
            EnemyScalingSystem.scalingTierForEncounter(encounterIndex)

        val adaptationPrepared = AdaptationExposureSystem.beginEncounter(
            state = preparedState,
            regionId = regionId
        )
        val combatStart = CombatSystem.startCombat(
            state = adaptationPrepared,
            enemyDefinitionIds = firstWaveEnemyIds,
            encounterDefinitionId = encounterDefinition.id,
            regionDefinitionId = regionId,
            scalingTier = scalingTier,
            context = context
        )
        val spawnedEnemyIds = combatStart.state.run.combat.enemies
            .map { it.instanceId }
            .sorted()

        val encounterState = EncounterState(
            definitionId = encounterDefinition.id,
            encounterIndex = encounterIndex,
            encounterSeed = encounterSeed,
            spawnedEnemyIds = spawnedEnemyIds,
            status = EncounterStatus.ACTIVE,
            rewardEligible = true
        )

        val updatedWorld = combatStart.state.run.world.copy(
            currentEncounter = encounterState
        )

        return EncounterTransitionResult(
            state = combatStart.state.copy(
                run = combatStart.state.run.copy(
                    world = updatedWorld
                )
            ),
            events = buildList {
                add(
                    EncounterStarted(
                        encounterDefinitionId = encounterDefinition.id,
                        encounterIndex = encounterIndex
                    )
                )
                addAll(combatStart.events)
                add(
                    EncounterWaveStarted(
                        encounterDefinitionId = encounterDefinition.id,
                        encounterIndex = encounterIndex,
                        wave = 1,
                        totalWaves = encounterDefinition.waves,
                        enemyInstanceIds = spawnedEnemyIds
                    )
                )
                encounterDefinition.bossId?.let { add(BossPhaseChanged(it, 1, encounterDefinition.waves)) }
            }
        )
    }

    fun retreat(
        state: GameState
    ): EncounterTransitionResult {
        val encounter = state.run.world.currentEncounter
            ?: error("No encounter exists to retreat from")
        require(encounter.status == EncounterStatus.ACTIVE) {
            "Only an ACTIVE encounter can be retreated"
        }
        require(state.run.combat.status == CombatStatus.ACTIVE) {
            "ACTIVE encounter must own ACTIVE combat before retreat"
        }

        val sequenceId = state.run.combat.combatSequenceId
        val updatedWorld = state.run.world.copy(
            currentEncounter = encounter.copy(
                status = EncounterStatus.RETREATED,
                rewardEligible = false
            )
        )
        val updatedCombat = CombatState(
            status = CombatStatus.IDLE,
            combatSequenceId = sequenceId
        )

        val retreatedState = state.copy(
            run = state.run.copy(
                world = updatedWorld,
                combat = updatedCombat
            )
        )
        val adaptationState = state.run.world.activeRegionId
            ?.let { regionId ->
                AdaptationExposureSystem.discardCurrentEncounter(
                    state = retreatedState,
                    regionId = regionId
                )
            }
            ?: retreatedState

        val queued = state.run.combat.queuedPlayerAction
        return EncounterTransitionResult(
            state = adaptationState,
            events = buildList {
                add(
                    CombatEnded(
                        combatSequenceId = sequenceId,
                        reason = CombatEndReason.RETREATED
                    )
                )
                if (queued is QueuedPlayerAction.Skill) {
                    add(
                        SkillCastQueueCleared(
                            skillId = queued.skillId,
                            reason = SkillQueueClearReason.COMBAT_ENDED
                        )
                    )
                }
                add(
                    EncounterRetreated(
                        encounterDefinitionId = encounter.definitionId,
                        encounterIndex = encounter.encounterIndex
                    )
                )
            }
        )
    }

    fun resolvePlayerDefeat(
        state: GameState
    ): EncounterTransitionResult {
        val encounter = state.run.world.currentEncounter
            ?: error("Player defeat requires currentEncounter")
        require(encounter.status == EncounterStatus.ACTIVE) {
            "Player defeat requires ACTIVE currentEncounter"
        }
        val player = state.run.combat.playerCombatant
            ?: error("Player defeat requires playerCombatant")
        require(player.currentHealth == GameNumber.ZERO) {
            "Player defeat requires zero player health"
        }
        val sequenceId = state.run.combat.combatSequenceId
        val failedEncounter = encounter.copy(
            status = EncounterStatus.FAILED,
            rewardEligible = false
        )
        val terminalCombat = state.run.combat.copy(
            status = CombatStatus.DEFEAT,
            nextPlayerDecisionAt = null,
            nextEnemyDecisionAt = emptyMap(),
            queuedPlayerAction = null
        )
        val failedState = state.copy(
            run = state.run.copy(
                player = state.run.player.copy(currentHealth = GameNumber.ZERO),
                world = state.run.world.copy(currentEncounter = failedEncounter),
                combat = terminalCombat
            )
        )
        val adapted = state.run.world.activeRegionId?.let { regionId ->
            AdaptationExposureSystem.discardCurrentEncounter(failedState, regionId)
        } ?: failedState
        return EncounterTransitionResult(
            state = adapted,
            events = listOf(
                PlayerDefeated(player.instanceId),
                CombatEnded(sequenceId, CombatEndReason.DEFEAT),
                EncounterFailed(encounter.definitionId, encounter.encounterIndex)
            )
        )
    }

    fun resolveEnemyDeaths(
        state: GameState,
        deadEnemies: List<EnemyState>,
        killerInstanceId: InstanceId,
        context: EngineContext
    ): EncounterDeathTransition {
        val encounter = state.run.world.currentEncounter
            ?: error("Enemy death resolution requires currentEncounter")
        require(encounter.status == EncounterStatus.ACTIVE) {
            "Enemy death resolution requires ACTIVE currentEncounter"
        }
        val encounterDefinition = context.contentRegistry.encounter(encounter.definitionId)
        require(encounter.currentWave <= encounterDefinition.waves) {
            "Encounter wave exceeds authored wave count"
        }
        require(deadEnemies.isNotEmpty()) { "At least one dead enemy is required" }
        val orderedDead = deadEnemies.sortedBy { it.instanceId }
        val deadIds = orderedDead.map { it.instanceId }.toSet()
        require(orderedDead.all { it.instanceId in encounter.spawnedEnemyIds }) {
            "Killed enemy is not owned by currentEncounter"
        }
        require(orderedDead.all { it.combatant.currentHealth == GameNumber.ZERO }) {
            "Enemy death resolution requires zero-health enemies"
        }
        val livingEnemies = state.run.combat.enemies
            .filter { it.instanceId !in deadIds && it.combatant.currentHealth > GameNumber.ZERO }
            .sortedBy { it.instanceId }
        val killEvents = orderedDead.map { enemy ->
            EnemyKilled(enemy.instanceId, enemy.definitionId, killerInstanceId)
        }

        if (livingEnemies.isNotEmpty()) {
            var transitioned = state.copy(
                run = state.run.copy(
                    combat = state.run.combat.copy(
                        enemies = livingEnemies,
                        nextEnemyDecisionAt = state.run.combat.nextEnemyDecisionAt
                            .filterKeys { it !in deadIds }
                    )
                )
            )
            val rewardEvents = mutableListOf<GameEvent>()
            if (encounter.rewardEligible) {
                orderedDead.forEach { enemy ->
                    val reward = RewardSystem.grantEnemyDefeatReward(
                        state = transitioned,
                        enemyDefinition = context.contentRegistry.enemy(enemy.definitionId),
                        enemy = enemy,
                        context = context
                    )
                    transitioned = reward.state
                    rewardEvents += reward.events
                }
            }
            return EncounterDeathTransition(
                state = transitioned,
                events = killEvents + rewardEvents,
                encounterCleared = false
            )
        }

        if (encounter.currentWave < encounterDefinition.waves) {
            var transitioned = state
            val rewardEvents = mutableListOf<GameEvent>()
            if (encounter.rewardEligible) {
                orderedDead.forEach { enemy ->
                    val reward = RewardSystem.grantEnemyDefeatReward(
                        state = transitioned,
                        enemyDefinition = context.contentRegistry.enemy(enemy.definitionId),
                        enemy = enemy,
                        context = context
                    )
                    transitioned = reward.state
                    rewardEvents += reward.events
                }
            }
            val nextWave = encounter.currentWave + 1
            val scalingTier = orderedDead.first().scalingTier
            val nextWaveEnemyIds = encounterDefinition.enemyDefinitionIdsForWave(nextWave)
            val creations = nextWaveEnemyIds.map { definitionId ->
                EnemyFactory.create(
                    state = transitioned,
                    definitionId = definitionId,
                    context = context,
                    scalingTier = scalingTier,
                    regionDefinitionId = encounterDefinition.regionId,
                    activeTraitIds = encounterDefinition.eliteModifiers.map { it.id }.toSortedSet()
                )
            }
            val spawned = creations.map { it.enemy }.sortedBy { it.instanceId }
            val enemyDeadlines = spawned.mapNotNull { enemy ->
                val definition = context.contentRegistry.enemy(enemy.definitionId)
                val attackId = definition.attackDefinitionId ?: return@mapNotNull null
                val attack = context.contentRegistry.enemyAttack(attackId)
                enemy.instanceId to (
                    transitioned.engine.simulationTime + MutationSystem.enemyActionInterval(
                        com.idlerpg.game.domain.system.enemy.EnemyEliteSystem.attackInterval(
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
            transitioned = transitioned.copy(
                run = transitioned.run.copy(
                    combat = transitioned.run.combat.copy(
                        status = CombatStatus.ACTIVE,
                        enemies = spawned,
                        nextEnemyDecisionAt = enemyDeadlines
                    ),
                    world = transitioned.run.world.copy(
                        currentEncounter = encounter.copy(
                            currentWave = nextWave,
                            spawnedEnemyIds = encounter.spawnedEnemyIds +
                                spawned.map { it.instanceId }
                        )
                    )
                )
            )
            return EncounterDeathTransition(
                state = transitioned,
                events = buildList {
                    addAll(killEvents)
                    addAll(rewardEvents)
                    creations.forEach { addAll(it.events) }
                    add(
                        EncounterWaveStarted(
                            encounterDefinitionId = encounter.definitionId,
                            encounterIndex = encounter.encounterIndex,
                            wave = nextWave,
                            totalWaves = encounterDefinition.waves,
                            enemyInstanceIds = spawned.map { it.instanceId }
                        )
                    )
                    encounterDefinition.bossId?.let { add(BossPhaseChanged(it, nextWave, encounterDefinition.waves)) }
                },
                encounterCleared = false
            )
        }

        val sequenceId = state.run.combat.combatSequenceId
        var transitioned = state.copy(
            run = state.run.copy(
                combat = state.run.combat.copy(
                    status = CombatStatus.VICTORY,
                    nextPlayerDecisionAt = null,
                    nextEnemyDecisionAt = emptyMap()
                ),
                world = state.run.world.copy(
                    currentEncounter = encounter.copy(
                        status = EncounterStatus.CLEARED,
                        rewardEligible = false
                    )
                )
            )
        )
        val adaptation = AdaptationSystem.completeEncounter(
            state = transitioned,
            regionId = encounterDefinition.regionId,
            contentRegistry = context.contentRegistry,
            balanceConfig = context.balanceConfig
        )
        transitioned = adaptation.state
        val rewardEvents = mutableListOf<GameEvent>()
        if (encounter.rewardEligible) {
            orderedDead.forEach { enemy ->
                val reward = RewardSystem.grantEnemyDefeatReward(
                    state = transitioned,
                    enemyDefinition = context.contentRegistry.enemy(enemy.definitionId),
                    enemy = enemy,
                    context = context
                )
                transitioned = reward.state
                rewardEvents += reward.events
            }
        }
        val regionClear = RegionSystem.recordEncounterClear(
            state = transitioned,
            encounterDefinition = encounterDefinition,
            encounterIndex = encounter.encounterIndex,
            contentRegistry = context.contentRegistry
        )
        transitioned = regionClear.state.copy(
            run = regionClear.state.run.copy(
                player = regionClear.state.run.player.copy(
                    currentHealth = com.idlerpg.game.domain.system.stats.DerivedStatSystem
                        .maximumHealth(regionClear.state, context.contentRegistry)
                ),
                combat = regionClear.state.run.combat.copy(
                    playerCombatant = regionClear.state.run.combat.playerCombatant?.copy(
                        currentHealth = com.idlerpg.game.domain.system.stats.DerivedStatSystem
                            .maximumHealth(regionClear.state, context.contentRegistry)
                    )
                )
            )
        )
        val terminalEvents = buildList {
            addAll(killEvents)
            add(CombatEnded(sequenceId, CombatEndReason.VICTORY))
            add(EncounterCleared(encounter.definitionId, encounter.encounterIndex))
            addAll(adaptation.events)
            addAll(rewardEvents)
            addAll(regionClear.events)
            if (encounterDefinition.nextEncounterId != null &&
                InventoryCapacitySystem.isProgressionBlocked(
                    transitioned.run.inventory,
                    context.balanceConfig
                )
            ) {
                val inventory = transitioned.run.inventory
                add(
                    InventoryProgressionBlocked(
                        normalItemCount = inventory.itemsById.size.toLong(),
                        normalCapacity = InventoryCapacitySystem.effectiveSlotCapacity(inventory),
                        overflowItemCount = inventory.overflowItemsById.size.toLong(),
                        overflowCapacity = context.balanceConfig.inventoryOverflowCapacity
                    )
                )
            }
        }
        return EncounterDeathTransition(transitioned, terminalEvents, encounterCleared = true)
    }

    /**
     * Pure content-aware projection used by WorldSystem.
     *
     * It consumes no RNG, allocates no IDs, and mutates no state.
     */
    fun scheduledActions(
        state: GameState,
        contentRegistry: ContentRegistry,
        balanceConfig: BalanceConfig
    ): List<ScheduledAction> {
        val encounter = state.run.world.currentEncounter ?: return emptyList()
        if (encounter.status != EncounterStatus.CLEARED && encounter.status != EncounterStatus.FAILED) {
            return emptyList()
        }
        val nextEncounterId = automationTarget(state, contentRegistry) ?: return emptyList()
        if (InventoryCapacitySystem.isProgressionBlocked(
                state.run.inventory,
                balanceConfig
            )
        ) {
            return emptyList()
        }

        return listOf(
            ScheduledAction(
                dueAt = state.engine.simulationTime,
                type = ScheduledActionType.ENCOUNTER_LIFECYCLE,
                stableTieBreakKey = encounter.encounterIndex,
                sourceContentId = nextEncounterId
            )
        )
    }

    override fun execute(
        state: GameState,
        action: ScheduledAction,
        context: EngineContext
    ): ScheduledActionExecution {
        require(action.type == ScheduledActionType.ENCOUNTER_LIFECYCLE) {
            "EncounterSystem cannot execute ${action.type}"
        }
        require(action.dueAt == state.engine.simulationTime) {
            "Encounter lifecycle action must execute at current simulation time"
        }

        val cleared = state.run.world.currentEncounter
            ?: error("ENCOUNTER_LIFECYCLE requires currentEncounter")
        require(cleared.status == EncounterStatus.CLEARED || cleared.status == EncounterStatus.FAILED) {
            "ENCOUNTER_LIFECYCLE requires terminal currentEncounter"
        }
        val nextEncounterId = automationTarget(state, context.contentRegistry)
            ?: error("Terminal encounter has no automation target")
        require(action.sourceContentId == nextEncounterId) {
            "Scheduled next encounter does not match authored continuation"
        }

        val startState = if (cleared.status == EncounterStatus.FAILED &&
            state.run.world.pushFailurePolicy !=
            com.idlerpg.game.domain.model.world.PushFailurePolicy.RETRY &&
            nextEncounterId in state.run.world.clearedEncounterIds
        ) {
            state.copy(
                run = state.run.copy(
                    world = state.run.world.copy(
                        automationMode = com.idlerpg.game.domain.model.world.WorldAutomationMode.FARM,
                        selectedFarmEncounterId = nextEncounterId
                    )
                )
            )
        } else state
        val transition = startEncounter(
            state = startState,
            encounterDefinitionId = nextEncounterId,
            context = context
        )
        return ScheduledActionExecution(
            state = transition.state,
            events = transition.events
        )
    }

    private fun automationTarget(
        state: GameState,
        contentRegistry: ContentRegistry
    ): ContentId? {
        val encounter = state.run.world.currentEncounter ?: return null
        val world = state.run.world
        val activeRegionId = world.activeRegionId ?: return null
        val activeRegion = contentRegistry.regionOrNull(activeRegionId) ?: return null

        fun isAuthoredInActiveRegion(encounterId: ContentId): Boolean {
            val definition = contentRegistry.encounterOrNull(encounterId) ?: return false
            return definition.regionId == activeRegionId &&
                encounterId in activeRegion.encounterIds
        }

        fun validClearedFarmTarget(): ContentId? =
            world.selectedFarmEncounterId?.takeIf { candidate ->
                candidate in world.clearedEncounterIds &&
                    isAuthoredInActiveRegion(candidate)
            }

        val currentTarget = encounter.definitionId.takeIf { isAuthoredInActiveRegion(it) }
        if (world.automationMode == com.idlerpg.game.domain.model.world.WorldAutomationMode.FARM) {
            // A legacy/corrupt save may carry an unknown or cross-region farm ID. Fall back
            // to the terminal encounter instead of allowing startEncounter to throw while the
            // autonomous scheduler is running.
            return validClearedFarmTarget() ?: currentTarget
        }
        if (encounter.status == EncounterStatus.CLEARED) {
            return activeRegion.encounterIds
                .firstOrNull {
                    it !in world.clearedEncounterIds && isAuthoredInActiveRegion(it)
                }
                ?: contentRegistry.encounterOrNull(encounter.definitionId)
                    ?.nextEncounterId
                    ?.takeIf { isAuthoredInActiveRegion(it) }
        }
        return when (world.pushFailurePolicy) {
            com.idlerpg.game.domain.model.world.PushFailurePolicy.RETRY -> currentTarget
            com.idlerpg.game.domain.model.world.PushFailurePolicy.FALL_BACK_TO_SELECTED_FARM ->
                validClearedFarmTarget() ?: currentTarget
            com.idlerpg.game.domain.model.world.PushFailurePolicy.FARM_HIGHEST_CLEARED -> {
                activeRegion.encounterIds
                    .filter {
                        it in world.clearedEncounterIds && isAuthoredInActiveRegion(it)
                    }
                    .lastOrNull() ?: currentTarget
            }
        }
    }
}
