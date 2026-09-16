package com.idlerpg.game.simulation

import com.idlerpg.game.application.GameRuntime
import com.idlerpg.game.application.OfflineSessionCoordinator
import com.idlerpg.game.core.id.ContentId
import com.idlerpg.game.core.id.InstanceId
import com.idlerpg.game.core.number.GameNumber
import com.idlerpg.game.core.number.Ratio
import com.idlerpg.game.core.time.GameDuration
import com.idlerpg.game.data.content.DefaultGameContent
import com.idlerpg.game.data.local.SaveData
import com.idlerpg.game.data.local.SaveEnvelope
import com.idlerpg.game.domain.command.StartEncounter
import com.idlerpg.game.domain.definition.Affinity
import com.idlerpg.game.domain.definition.DamageKind
import com.idlerpg.game.domain.definition.world.EncounterDefinition
import com.idlerpg.game.domain.definition.combat.EffectSpec
import com.idlerpg.game.domain.definition.combat.SkillDefinition
import com.idlerpg.game.domain.engine.EngineContext
import com.idlerpg.game.domain.event.CombatEnded
import com.idlerpg.game.domain.event.CombatStarted
import com.idlerpg.game.domain.event.CurrencyGranted
import com.idlerpg.game.domain.event.DamageDealt
import com.idlerpg.game.domain.event.EncounterCleared
import com.idlerpg.game.domain.event.EncounterWaveStarted
import com.idlerpg.game.domain.event.EnemyKilled
import com.idlerpg.game.domain.event.ExperienceGranted
import com.idlerpg.game.domain.event.SkillUsed
import com.idlerpg.game.domain.model.world.EncounterState
import com.idlerpg.game.domain.model.world.EncounterStatus
import com.idlerpg.game.domain.system.combat.ActionResolutionSystem
import com.idlerpg.game.domain.system.combat.CombatSystem
import com.idlerpg.game.domain.system.combat.TargetingSystem

/** Production lifecycle plus stable targeting and bounded multi-hit/AoE regressions. */
object MultiEnemyCombatScenarioTest {
    fun run() {
        authoredMultiEnemyEncounterClearsWithoutSingleEnemyCrash()
        directCombatRejectsUnsupportedFormation()
        activeAndOfflineWaveTransitionsAreEquivalent()
        primitiveTargetsAndEventsAreStable()
    }

    private fun directCombatRejectsUnsupportedFormation() {
        val runtime = SimulationTestSupport.runtime(seed = 1_305L)
        val factory = SimulationTestSupport.factory()
        val failure = runCatching {
            CombatSystem.startCombat(
                state = runtime.state(),
                enemyDefinitionIds = List(EncounterDefinition.MAX_ACTIVE_ENEMIES + 1) {
                    DefaultGameContent.SLIME_ID
                },
                encounterDefinitionId = DefaultGameContent.TRAINING_SLIME_ENCOUNTER_ID,
                regionDefinitionId = DefaultGameContent.TRAINING_HOLLOW_REGION_ID,
                scalingTier = 0L,
                context = factory.createEngineContext()
            )
        }.exceptionOrNull()
        check(failure is IllegalArgumentException) {
            "Direct combat must reject formations above the shared active-enemy limit"
        }
    }

    private fun authoredMultiEnemyEncounterClearsWithoutSingleEnemyCrash() {
        val (runtime, startResult) = startBulwark(seed = 1_301L)
        val initialWave = startResult.events.map { it.event }
            .filterIsInstance<EncounterWaveStarted>().single()
        check(initialWave.wave == 1 && initialWave.totalWaves == 2)
        check(runtime.state().run.combat.enemies.size == 1)
        val firstWaveIds = runtime.state().run.combat.enemies.map { it.instanceId }.toSet()
        val sequenceId = runtime.state().run.combat.combatSequenceId

        val partial = runtime.advance(GameDuration.ofSeconds(1L))
        check(partial.events.count { it.event is EnemyKilled } == 1)
        val waveEvent = partial.events.map { it.event }
            .filterIsInstance<EncounterWaveStarted>().single()
        check(waveEvent.wave == 2 && waveEvent.totalWaves == 2)
        check(waveEvent.enemyInstanceIds.none { it in firstWaveIds })
        check(runtime.state().run.world.currentEncounter?.currentWave == 2)
        check(runtime.state().run.combat.enemies.size == 1)
        check(emberExposure(runtime).currentEncounterContribution == GameNumber.of(77L))
        check(emberExposure(runtime).pressure == GameNumber.ZERO)
        check(runtime.state().run.world.regionProgressById[
            DefaultGameContent.TRAINING_HOLLOW_REGION_ID
        ]?.highestClearedEncounterTier == 3L)

        check(runtime.state().run.combat.combatSequenceId == sequenceId)
        check(emberExposure(runtime).currentEncounterContribution == GameNumber.of(77L))
        check(emberExposure(runtime).pressure == GameNumber.ZERO)
        check(SaveData.fromGameState(runtime.state()).toGameState() == runtime.state())

        val final = runtime.advance(GameDuration.ofSeconds(2L))
        val allEvents = partial.events + final.events
        check(allEvents.count { it.event is EnemyKilled } == 2)
        check(allEvents.count { it.event is CurrencyGranted } == 2)
        check(allEvents.count { it.event is ExperienceGranted } == 2)
        check(allEvents.count {
            (it.event as? CombatEnded)?.combatSequenceId == sequenceId
        } == 1)
        check(allEvents.count {
            (it.event as? EncounterCleared)?.encounterDefinitionId ==
                DefaultGameContent.HOLLOW_BULWARK_ENCOUNTER_ID
        } == 1)
        check(runtime.state().run.world.regionProgressById[
            DefaultGameContent.TRAINING_HOLLOW_REGION_ID
        ]?.highestClearedEncounterTier == 4L)
        check(emberExposure(runtime).currentEncounterContribution == GameNumber.ZERO)
        check(emberExposure(runtime).recentEncounterContribution == GameNumber.of(77L))
        check(emberExposure(runtime).pressure > GameNumber.ZERO)
    }

    private fun startBulwark(seed: Long): Pair<GameRuntime, com.idlerpg.game.domain.engine.EngineResult> {
        val runtime = SimulationTestSupport.runtime(seed = seed)
        val initial = runtime.state()
        val progress = initial.run.world.regionProgressById[
            DefaultGameContent.TRAINING_HOLLOW_REGION_ID
        ] ?: error("Missing Training Hollow progress")
        runtime.replaceLoadedState(
            initial.copy(
                run = initial.run.copy(
                    player = initial.run.player.copy(
                        baseStats = initial.run.player.baseStats.copy(
                            attackPower = GameNumber.of(1_000L)
                        )
                    ),
                    world = initial.run.world.copy(
                        activeRegionId = DefaultGameContent.TRAINING_HOLLOW_REGION_ID,
                        currentEncounter = EncounterState(
                            definitionId = DefaultGameContent.CINDER_WISP_ENCOUNTER_ID,
                            encounterIndex = 3L,
                            encounterSeed = seed,
                            status = EncounterStatus.CLEARED,
                            rewardEligible = false
                        ),
                        regionProgressById = initial.run.world.regionProgressById +
                            (DefaultGameContent.TRAINING_HOLLOW_REGION_ID to progress.copy(
                                highestClearedEncounterTier = 3L
                            ))
                    )
                )
            )
        )
        val startResult = runtime.dispatch(
            StartEncounter(DefaultGameContent.HOLLOW_BULWARK_ENCOUNTER_ID)
        )
        SimulationTestSupport.checkAccepted(startResult)
        check(startResult.events.map { it.event }.filterIsInstance<CombatStarted>().size == 1)
        val started = runtime.state()
        runtime.replaceLoadedState(
            started.copy(
                run = started.run.copy(
                    adaptation = started.run.adaptation.copy(
                        regionStateById = started.run.adaptation.regionStateById +
                            (DefaultGameContent.TRAINING_HOLLOW_REGION_ID to
                                com.idlerpg.game.domain.model.adaptation.RegionAdaptationState(
                                    exposureByAffinityId = mapOf(
                                        Affinity.EMBER.id to
                                            com.idlerpg.game.domain.model.adaptation.AffinityExposureState(
                                                currentEncounterContribution = GameNumber.of(77L)
                                            )
                                    )
                                ))
                    )
                )
            )
        )
        return runtime to startResult
    }

    private fun emberExposure(runtime: GameRuntime) =
        runtime.state().run.adaptation.regionStateById
            .getValue(DefaultGameContent.TRAINING_HOLLOW_REGION_ID)
            .exposureByAffinityId
            .getValue(Affinity.EMBER.id)

    private fun activeAndOfflineWaveTransitionsAreEquivalent() {
        val (setup, _) = startBulwark(seed = 1_304L)
        val startingState = setup.state()
        val factory = SimulationTestSupport.factory()
        val foreground = GameRuntime(factory.loadedGame(startingState), factory)
        val foregroundResult = foreground.advance(GameDuration.ofSeconds(4L))
        check(foregroundResult.events.any { it.event is EncounterWaveStarted })

        val savedAt = 8_000_000L
        val repository = SimulationTestSupport.InMemoryGameRepository(
            SaveEnvelope.create(
                gameState = startingState,
                contentVersion = SimulationTestSupport.CONTENT_VERSION,
                writtenAtEpochMs = savedAt
            )
        )
        val offline = OfflineSessionCoordinator(
            repository = repository,
            clock = SimulationTestSupport.MutableClock(savedAt + 4_000L),
            engineContext = factory.createEngineContext()
        ).resume() ?: error("Expected offline wave simulation")
        check(foregroundResult.state == offline.engineResult.state)
        check(foregroundResult.events == offline.engineResult.events)
    }

    private fun primitiveTargetsAndEventsAreStable() {
        val runtime = SimulationTestSupport.runtime(seed = 1_302L)
        SimulationTestSupport.startTraining(runtime)
        val started = runtime.state()
        val original = started.run.combat.enemies.single()
        val secondId = InstanceId(original.instanceId.value + 10_000L)
        val second = original.copy(
            instanceId = secondId,
            combatant = original.combatant.copy(
                instanceId = secondId,
                currentHealth = GameNumber.of(30L)
            )
        )
        val multi = started.copy(run = started.run.copy(combat = started.run.combat.copy(
            enemies = listOf(second, original)
        )))
        check(TargetingSystem.primaryLivingEnemy(multi.run.combat)?.instanceId == original.instanceId)
        check(TargetingSystem.lowestHealthEnemy(multi.run.combat)?.instanceId == secondId)
        check(TargetingSystem.highestHealthEnemy(multi.run.combat)?.instanceId == original.instanceId)

        val playerId = multi.run.combat.playerCombatant?.instanceId ?: error("Missing player")
        val effect = EffectSpec.DealDamage(
            powerRatio = Ratio.HALF,
            damageKind = DamageKind.PHYSICAL,
            canCritical = false,
            targetPattern = EffectSpec.TargetPattern.ALL_ENEMIES,
            hitCount = 2
        )
        val result = ActionResolutionSystem.resolvePrimitiveEffects(
            state = multi,
            actorInstanceId = playerId,
            targetInstanceId = original.instanceId,
            effects = listOf(effect),
            context = EngineContext(contentRegistry = SimulationTestSupport.factory().contentRegistry)
        )
        val hits = result.events.filterIsInstance<DamageDealt>()
        check(hits.size == 4)
        check(hits.map { it.targetInstanceId } == listOf(original.instanceId, secondId, original.instanceId, secondId))
        check(result.state.run.combat.enemies.single { it.instanceId == original.instanceId }.combatant.currentHealth == GameNumber.of(90L))
        check(result.state.run.combat.enemies.single { it.instanceId == secondId }.combatant.currentHealth == GameNumber.of(20L))

        val skill = SkillDefinition(
            id = ContentId("skill.test.arcane_sweep"),
            effects = listOf(effect),
            requiresEquipped = false
        )
        val skillResult = ActionResolutionSystem.resolveSkill(
            state = multi,
            actorInstanceId = playerId,
            targetInstanceId = original.instanceId,
            definition = skill,
            context = EngineContext(contentRegistry = SimulationTestSupport.factory().contentRegistry)
        )
        val used = skillResult.events.filterIsInstance<SkillUsed>().single()
        check(used.targetInstanceIds == listOf(original.instanceId, secondId))

        val invalidTarget = InstanceId(secondId.value + 1L)
        val failure = runCatching {
            ActionResolutionSystem.resolvePrimitiveEffects(
                state = multi,
                actorInstanceId = playerId,
                targetInstanceId = invalidTarget,
                effects = listOf(EffectSpec.DealDamage(canCritical = false)),
                context = EngineContext(contentRegistry = SimulationTestSupport.factory().contentRegistry)
            )
        }.exceptionOrNull()
        check(failure is IllegalStateException)
    }
}
