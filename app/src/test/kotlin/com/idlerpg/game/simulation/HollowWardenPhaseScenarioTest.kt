package com.idlerpg.game.simulation

import com.idlerpg.game.application.GameRuntime
import com.idlerpg.game.core.number.GameNumber
import com.idlerpg.game.core.id.InstanceId
import com.idlerpg.game.core.time.GameDuration
import com.idlerpg.game.core.time.GameTime
import com.idlerpg.game.data.content.DefaultGameContent
import com.idlerpg.game.data.content.HollowWardenContent
import com.idlerpg.game.data.content.TrainingHollowStrategyContent
import com.idlerpg.game.data.content.TrainingHollowWorldContent
import com.idlerpg.game.domain.command.StartEncounter
import com.idlerpg.game.domain.definition.world.EncounterDefinition
import com.idlerpg.game.domain.definition.world.EncounterType
import com.idlerpg.game.domain.event.BossPhaseChanged
import com.idlerpg.game.domain.event.EncounterCleared
import com.idlerpg.game.domain.event.GameEventEnvelope
import com.idlerpg.game.domain.event.SkillCastQueued
import com.idlerpg.game.domain.event.PlayerDefeated
import com.idlerpg.game.domain.model.GameState
import com.idlerpg.game.domain.model.world.EncounterState
import com.idlerpg.game.domain.model.world.EncounterStatus
import com.idlerpg.game.domain.model.world.RegionProgressState
import com.idlerpg.game.domain.model.world.WorldAutomationMode
import com.idlerpg.game.domain.model.world.WorldState
import com.idlerpg.game.presentation.content.PresentationContentRegistry
import com.idlerpg.game.presentation.event.GameEventPresenter
import com.idlerpg.game.presentation.model.BattleBossPhaseUi
import com.idlerpg.game.presentation.model.BattleFeedbackKind
import com.idlerpg.game.presentation.projection.BattleProjector
import com.idlerpg.game.presentation.query.DefaultGameReadQueries
import com.idlerpg.game.presentation.runtime.RuntimeTransition

/** The Warden is three deterministic tactical phases, not one repeated enemy list. */
object HollowWardenPhaseScenarioTest {
    fun run() {
        authoredPhaseContract()
        val first = scenario(9_301L)
        val replay = scenario(9_301L)
        check(first == replay)
    }

    private fun authoredPhaseContract() {
        val encounter = SimulationTestSupport.factory().contentRegistry.encounter(
            TrainingHollowWorldContent.stageId(30)
        )
        check(encounter.waves == 3)
        check(encounter.enemyDefinitionIdsForWave(1) == listOf(
            HollowWardenContent.ENEMY_ID,
            DefaultGameContent.HOLLOW_BULWARK_ID
        ))
        check(encounter.enemyDefinitionIdsForWave(2) == listOf(
            HollowWardenContent.ENEMY_ID,
            TrainingHollowStrategyContent.SHADE_MIMIC_ID
        ))
        check(encounter.enemyDefinitionIdsForWave(3) == listOf(
            HollowWardenContent.ENEMY_ID,
            TrainingHollowStrategyContent.ECHO_LEECH_ID,
            DefaultGameContent.ARCANE_SEER_ID
        ))
        val sharedFormation = EncounterDefinition(
            id = TrainingHollowWorldContent.stageId(97),
            regionId = DefaultGameContent.TRAINING_HOLLOW_REGION_ID,
            type = EncounterType.NORMAL,
            enemyDefinitionIds = listOf(DefaultGameContent.SLIME_ID),
            waves = 3
        )
        check((1..3).all {
            sharedFormation.enemyDefinitionIdsForWave(it) == listOf(DefaultGameContent.SLIME_ID)
        })
        check(runCatching {
            EncounterDefinition(
                id = TrainingHollowWorldContent.stageId(99),
                regionId = DefaultGameContent.TRAINING_HOLLOW_REGION_ID,
                type = EncounterType.NORMAL,
                enemyDefinitionIds = listOf(DefaultGameContent.SLIME_ID),
                waves = 2,
                waveEnemyDefinitionIds = listOf(listOf(DefaultGameContent.SLIME_ID))
            )
        }.isFailure)
        check(runCatching {
            EncounterDefinition(
                id = TrainingHollowWorldContent.stageId(98),
                regionId = DefaultGameContent.TRAINING_HOLLOW_REGION_ID,
                type = EncounterType.NORMAL,
                enemyDefinitionIds = listOf(DefaultGameContent.SLIME_ID),
                waveEnemyDefinitionIds = listOf(emptyList())
            )
        }.isFailure)
        val priorityFeedback = checkNotNull(GameEventPresenter().presentBattle(
            RuntimeTransition(
                state = GameState.newGame(9_399L),
                events = listOf(
                    GameEventEnvelope(
                        sequenceNumber = 1L,
                        simulationTime = GameTime.ZERO,
                        event = BossPhaseChanged(HollowWardenContent.BOSS_ID, 2, 3)
                    ),
                    GameEventEnvelope(
                        sequenceNumber = 2L,
                        simulationTime = GameTime.ZERO,
                        event = SkillCastQueued(DefaultGameContent.HEAVY_STRIKE_ID)
                    )
                ),
                commandResult = null
            )
        ))
        check(priorityFeedback.kind == BattleFeedbackKind.BOSS_PHASE)
        check(priorityFeedback.bossPhase == 2)
        val terminalFeedback = checkNotNull(GameEventPresenter().presentBattle(
            RuntimeTransition(
                state = GameState.newGame(9_400L),
                events = listOf(
                    GameEventEnvelope(
                        sequenceNumber = 1L,
                        simulationTime = GameTime.ZERO,
                        event = BossPhaseChanged(HollowWardenContent.BOSS_ID, 2, 3)
                    ),
                    GameEventEnvelope(
                        sequenceNumber = 2L,
                        simulationTime = GameTime.ZERO,
                        event = PlayerDefeated(InstanceId(1L))
                    )
                ),
                commandResult = null
            )
        ))
        check(terminalFeedback.kind == BattleFeedbackKind.PLAYER_DEFEATED)
    }

    private fun scenario(seed: Long): PhaseRun {
        val factory = SimulationTestSupport.factory()
        val runtime = GameRuntime(factory.newGame(seed), factory)
        val initial = runtime.state()
        val maximum = GameNumber.of(1_000_000L)
        runtime.replaceLoadedState(initial.copy(run = initial.run.copy(
            player = initial.run.player.copy(
                currentHealth = maximum,
                baseStats = initial.run.player.baseStats.copy(
                    attackPower = maximum,
                    maxHealth = maximum
                )
            ),
            world = WorldState(
                activeRegionId = DefaultGameContent.TRAINING_HOLLOW_REGION_ID,
                unlockedRegionIds = setOf(DefaultGameContent.TRAINING_HOLLOW_REGION_ID),
                regionProgressById = mapOf(
                    DefaultGameContent.TRAINING_HOLLOW_REGION_ID to RegionProgressState(
                        highestClearedEncounterTier = 29L,
                        normalClears = GameNumber.of(15L)
                    )
                ),
                currentEncounter = EncounterState(
                    definitionId = TrainingHollowWorldContent.stageId(29),
                    encounterIndex = 29L,
                    encounterSeed = seed,
                    status = EncounterStatus.CLEARED,
                    rewardEligible = false
                ),
                automationMode = WorldAutomationMode.FARM,
                selectedFarmEncounterId = TrainingHollowWorldContent.stageId(29),
                clearedEncounterIds = setOf(TrainingHollowWorldContent.stageId(29))
            )
        )))

        val presenter = GameEventPresenter()
        val projector = BattleProjector(
            contentRegistry = factory.contentRegistry,
            presentationContentRegistry = PresentationContentRegistry.default(),
            readQueries = DefaultGameReadQueries(
                factory.contentRegistry,
                factory.gameConfig.balance,
                factory.createEngineContext()
            )
        )
        val formations = mutableListOf<List<String>>()
        val phases = mutableListOf<Int>()
        val feedbackPhases = mutableListOf<Int>()
        val projectedPhases = mutableListOf<BattleBossPhaseUi>()
        var cleared = 0

        fun record(result: com.idlerpg.game.domain.engine.EngineResult) {
            val phaseEvents = result.events.map { it.event }.filterIsInstance<BossPhaseChanged>()
            check(phaseEvents.size <= 1) { "Scenario step must not cross multiple boss phases" }
            phaseEvents.forEach { event ->
                phases += event.phase
                formations += runtime.state().run.combat.enemies.map { it.definitionId.value }
                val feedback = checkNotNull(presenter.presentBattle(
                    RuntimeTransition(result.state, result.events, result.commandResult)
                ))
                check(feedback.kind == BattleFeedbackKind.BOSS_PHASE)
                feedbackPhases += checkNotNull(feedback.bossPhase)
                projectedPhases += checkNotNull(projector.project(runtime.state()).bossPhase)
            }
            cleared += result.events.count {
                (it.event as? EncounterCleared)?.encounterDefinitionId ==
                    TrainingHollowWorldContent.stageId(30)
            }
        }

        val start = runtime.dispatch(StartEncounter(TrainingHollowWorldContent.stageId(30)))
        SimulationTestSupport.checkAccepted(start)
        record(start)
        repeat(300) {
            if (cleared == 0) {
                record(runtime.advance(GameDuration.ofMillis(100L)))
            }
        }
        check(cleared == 1)
        check(phases == listOf(1, 2, 3))
        check(feedbackPhases == phases)
        check(projectedPhases == listOf(
            BattleBossPhaseUi.BASTION,
            BattleBossPhaseUi.REFLECTION,
            BattleBossPhaseUi.FRACTURE
        ))
        check(formations == listOf(
            listOf("enemy.hollow_warden", "enemy.hollow_bulwark"),
            listOf("enemy.hollow_warden", "enemy.shade_mimic"),
            listOf("enemy.hollow_warden", "enemy.echo_leech", "enemy.arcane_seer")
        ))
        return PhaseRun(
            formations = formations.map { it.toList() },
            phases = phases.toList(),
            feedbackPhases = feedbackPhases.toList(),
            projectedPhases = projectedPhases.toList(),
            finalState = runtime.state()
        )
    }

    private data class PhaseRun(
        val formations: List<List<String>>,
        val phases: List<Int>,
        val feedbackPhases: List<Int>,
        val projectedPhases: List<BattleBossPhaseUi>,
        val finalState: GameState
    )
}
