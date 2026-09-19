package com.idlerpg.game.simulation

import com.idlerpg.game.application.AutosaveCoordinator
import com.idlerpg.game.application.GameRuntime
import com.idlerpg.game.application.OfflineSessionCoordinator
import com.idlerpg.game.application.OfflineStoppingReason
import com.idlerpg.game.core.time.GameDuration
import com.idlerpg.game.data.content.DefaultGameContent
import com.idlerpg.game.data.content.TrainingHollowWorldContent
import com.idlerpg.game.data.local.SaveEnvelope
import com.idlerpg.game.domain.definition.CurrencyId
import com.idlerpg.game.domain.definition.progression.FeatureUnlockScope
import com.idlerpg.game.domain.engine.SimulationEngine
import com.idlerpg.game.domain.event.CurrencyGranted
import com.idlerpg.game.domain.event.ExperienceGranted
import com.idlerpg.game.domain.event.FeatureUnlocked
import com.idlerpg.game.domain.event.GameEventEnvelope
import com.idlerpg.game.domain.event.PlayerLeveledUp
import com.idlerpg.game.domain.model.GameState
import com.idlerpg.game.domain.model.combat.CombatState
import com.idlerpg.game.domain.model.world.EncounterState
import com.idlerpg.game.domain.model.world.EncounterStatus
import com.idlerpg.game.domain.model.world.WorldAutomationMode

/**
 * Real save/coordinator/runtime boundary coverage for deterministic offline continuation.
 *
 * These checks intentionally cross the persisted checkpoint boundary instead of only
 * asserting a pure projection or a synthetic return summary.
 */
object OfflineBoundaryScenarioTest {
    fun run() {
        twelveHourCapAdvancesAndReanchorsCheckpoint()
        latestEligibleNonBossIsTheOfflineFarmTarget()
        runtimeResumeAndRecoveryAreForegroundSafe()
    }

    private fun twelveHourCapAdvancesAndReanchorsCheckpoint() {
        val factory = SimulationTestSupport.factory()
        val savedState = GameState.newGame(601L)
        val savedAt = 10_000L
        val requested = GameDuration.ofHours(24L)
        val repository = SimulationTestSupport.InMemoryGameRepository(
            SaveEnvelope.create(
                gameState = savedState,
                contentVersion = SimulationTestSupport.CONTENT_VERSION,
                writtenAtEpochMs = savedAt
            )
        )
        val clock = SimulationTestSupport.MutableClock(savedAt + requested.millis)

        val result = OfflineSessionCoordinator(
            repository = repository,
            clock = clock,
            engineContext = factory.createEngineContext()
        ).resume() ?: error("Expected capped offline resume")

        val capped = GameDuration.ofHours(12L)
        check(result.summary.requestedElapsed == requested)
        check(result.summary.simulatedElapsed == capped)
        check(result.summary.durationClamped)
        check(result.summary.stoppingReason == OfflineStoppingReason.NO_ELIGIBLE_FARM_STAGE)
        check(result.sourceWrittenAtEpochMs == savedAt)
        check(result.checkpointWrittenAtEpochMs == clock.epochMs)
        check(repository.envelope?.writtenAtEpochMs == clock.epochMs)
        check(
            result.state.engine.simulationTime ==
                savedState.engine.simulationTime + capped
        )
        check(repository.envelope?.gameState() == result.state)
    }

    private fun latestEligibleNonBossIsTheOfflineFarmTarget() {
        val factory = SimulationTestSupport.factory()
        val setup = GameRuntime(
            initialSession = factory.newGame(602L),
            sessionFactory = factory
        )
        SimulationTestSupport.startTraining(setup)
        val base = setup.state()
        val regionId = DefaultGameContent.TRAINING_HOLLOW_REGION_ID
        val stageNine = TrainingHollowWorldContent.stageId(9)
        val stageTen = TrainingHollowWorldContent.stageId(10)
        val stageThirtyBoss = TrainingHollowWorldContent.stageId(30)
        val region = factory.contentRegistry.region(regionId)
        val encounterIndex = region.encounterIds.indexOf(stageTen) + 1
        check(encounterIndex > 0)

        val before = base.copy(
            run = base.run.copy(
                world = base.run.world.copy(
                    currentEncounter = EncounterState(
                        definitionId = stageTen,
                        encounterIndex = encounterIndex.toLong(),
                        encounterSeed = 0L,
                        status = EncounterStatus.CLEARED
                    ),
                    automationMode = WorldAutomationMode.FARM,
                    selectedFarmEncounterId = null,
                    clearedEncounterIds = setOf(stageNine, stageTen, stageThirtyBoss)
                ),
                combat = CombatState()
            )
        )
        val elapsed = GameDuration.ofMinutes(5L)
        val savedAt = 20_000L
        val repository = SimulationTestSupport.InMemoryGameRepository(
            SaveEnvelope.create(
                gameState = before,
                contentVersion = SimulationTestSupport.CONTENT_VERSION,
                writtenAtEpochMs = savedAt
            )
        )
        val clock = SimulationTestSupport.MutableClock(savedAt + elapsed.millis)
        val result = OfflineSessionCoordinator(
            repository = repository,
            clock = clock,
            engineContext = factory.createEngineContext()
        ).resume() ?: error("Expected targeted offline resume")

        val canonicalInput = before.copy(
            run = before.run.copy(
                world = before.run.world.copy(
                    selectedFarmEncounterId = stageTen
                ),
                combat = CombatState()
            )
        )
        val expected = SimulationEngine.advance(
            state = canonicalInput,
            duration = elapsed,
            context = factory.createEngineContext()
        )
        val expectedOfflineEvents = expected.events.filter { envelope ->
            when (val event = envelope.event) {
                is ExperienceGranted,
                is PlayerLeveledUp -> true
                is FeatureUnlocked ->
                    factory.contentRegistry.featureUnlockOrNull(event.featureId)?.scope ==
                        FeatureUnlockScope.RUN
                is CurrencyGranted -> event.currencyId == CurrencyId.GOLD
                else -> false
            }
        }

        check(result.summary.startingStage == 10)
        check(result.summary.endingStage == 10)
        check(result.events == expectedOfflineEvents)
        check(result.state.run.world == before.run.world)
        check(result.state.engine.simulationTime == before.engine.simulationTime + elapsed)
    }

    private fun runtimeResumeAndRecoveryAreForegroundSafe() {
        val factory = SimulationTestSupport.factory()
        val setup = GameRuntime(
            initialSession = factory.newGame(603L),
            sessionFactory = factory
        )
        SimulationTestSupport.startTraining(setup)
        val startingState = setup.state()
        val elapsed = GameDuration.ofSeconds(60L)
        val savedAt = 30_000L
        val repository = SimulationTestSupport.InMemoryGameRepository(
            SaveEnvelope.create(
                gameState = startingState,
                contentVersion = SimulationTestSupport.CONTENT_VERSION,
                writtenAtEpochMs = savedAt
            )
        )
        val clock = SimulationTestSupport.MutableClock(savedAt + elapsed.millis)
        val runtime = GameRuntime(
            initialSession = factory.loadedGame(startingState),
            sessionFactory = factory,
            autosaveCoordinator = AutosaveCoordinator(
                repository = repository,
                clock = clock,
                contentVersion = SimulationTestSupport.CONTENT_VERSION
            ),
            offlineSessionCoordinator = OfflineSessionCoordinator(
                repository = repository,
                clock = clock,
                engineContext = factory.createEngineContext()
            )
        )

        val resumed = runtime.resumeOffline() ?: error("Expected runtime offline resume")
        check(runtime.state() == resumed.state)
        check(repository.envelope?.gameState() == resumed.state)
        check(repository.envelope?.writtenAtEpochMs == clock.epochMs)

        val foregroundTick = runtime.advance(GameDuration.ofMillis(1L))
        check(
            foregroundTick.state.engine.simulationTime ==
                resumed.state.engine.simulationTime + GameDuration.ofMillis(1L)
        )

        val mixedState = startingState.copy(
            engine = startingState.engine.copy(
                simulationTime = startingState.engine.simulationTime + elapsed
            )
        )
        val recoverySavedAt = 200_000L
        val recoveryRepository = SimulationTestSupport.InMemoryGameRepository(
            SaveEnvelope.create(
                gameState = mixedState,
                contentVersion = SimulationTestSupport.CONTENT_VERSION,
                writtenAtEpochMs = recoverySavedAt
            )
        )
        val recoveryClock = SimulationTestSupport.MutableClock(recoverySavedAt)
        val recoveryRuntime = GameRuntime(
            initialSession = factory.loadedGame(mixedState),
            sessionFactory = factory,
            autosaveCoordinator = AutosaveCoordinator(
                repository = recoveryRepository,
                clock = recoveryClock,
                contentVersion = SimulationTestSupport.CONTENT_VERSION
            ),
            offlineSessionCoordinator = OfflineSessionCoordinator(
                repository = recoveryRepository,
                clock = recoveryClock,
                engineContext = factory.createEngineContext()
            )
        )

        val recovered = recoveryRuntime.restoreSavedGameWithoutOfflineProgress()
            ?: error("Expected recovery checkpoint")
        check(recovered.run.combat.nextPlayerDecisionAt!! >= recovered.engine.simulationTime)
        check(
            recovered.run.combat.nextEnemyDecisionAt.values.all {
                it >= recovered.engine.simulationTime
            }
        )
        check(recoveryRepository.envelope?.gameState() == recovered)

        val recoveryTick = recoveryRuntime.advance(GameDuration.ofMillis(1L))
        check(
            recoveryTick.state.engine.simulationTime ==
                recovered.engine.simulationTime + GameDuration.ofMillis(1L)
        )
    }
}
