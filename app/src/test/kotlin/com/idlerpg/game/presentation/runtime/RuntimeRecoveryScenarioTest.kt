package com.idlerpg.game.presentation.runtime

import com.idlerpg.game.application.AutosaveCoordinator
import com.idlerpg.game.application.GameRuntime
import com.idlerpg.game.application.OfflineSessionCoordinator
import com.idlerpg.game.core.time.GameDuration
import com.idlerpg.game.data.local.SaveEnvelope
import com.idlerpg.game.data.local.SaveVersion
import com.idlerpg.game.data.repository.GameRepository
import com.idlerpg.game.domain.command.SetHeroName
import com.idlerpg.game.domain.model.GameState
import com.idlerpg.game.simulation.SimulationTestSupport
import java.util.concurrent.AbstractExecutorService
import java.util.concurrent.TimeUnit
import org.junit.Test

/**
 * Repository-backed production recovery flow for a legacy mixed-time offline checkpoint.
 *
 * The first test follows the player-visible path: Continue Expedition -> simulation failure ->
 * Open last confirmed save -> first foreground tick. The second test proves that a failed
 * recovery checkpoint does not publish the candidate session and can be retried.
 */
class RuntimeRecoveryScenarioTest {
    @Test
    fun activeCombatRecoveryRepairsCheckpointBeforeFirstForegroundTick() {
        val fixture = fixture()
        try {
            enterSimulationFailure(fixture)

            fixture.controller.continueWithSavedGame()

            assertRecoveredAndAdvances(fixture)
        } finally {
            fixture.controller.close()
        }
    }

    @Test
    fun recoveryCheckpointFailureKeepsOldSessionAndRemainsRetryable() {
        val fixture = fixture()
        try {
            enterSimulationFailure(fixture)
            fixture.repository.failSaves = true

            fixture.controller.continueWithSavedGame()

            check(fixture.controller.state.value.status == RuntimeHostStatus.ERROR)
            check(
                fixture.controller.state.value.failure?.kind ==
                    RuntimeFailureKind.SAVE
            )
            check(fixture.controller.state.value.saveStatus == RuntimeSaveStatus.ERROR)
            check(fixture.runtime.state() == fixture.mixedState)

            fixture.repository.failSaves = false
            fixture.controller.continueWithSavedGame()

            assertRecoveredAndAdvances(fixture)
        } finally {
            fixture.controller.close()
        }
    }

    private fun enterSimulationFailure(fixture: Fixture) {
        fixture.controller.initialize()
        check(fixture.controller.state.value.status == RuntimeHostStatus.MENU)

        fixture.controller.continueGame()
        check(fixture.controller.state.value.status == RuntimeHostStatus.READY)

        fixture.controller.advance(GameDuration.ofMillis(1L))
        check(fixture.controller.state.value.status == RuntimeHostStatus.ERROR)
        check(
            fixture.controller.state.value.failure?.kind ==
                RuntimeFailureKind.SIMULATION
        )
    }

    private fun assertRecoveredAndAdvances(fixture: Fixture) {
        val recovered = fixture.runtime.state()
        check(fixture.controller.state.value.status == RuntimeHostStatus.READY)
        check(fixture.controller.state.value.failure == null)
        check(recovered.engine.simulationTime == fixture.mixedState.engine.simulationTime)
        check(recovered.run.combat.nextPlayerDecisionAt!! >= recovered.engine.simulationTime)
        check(
            recovered.run.combat.nextEnemyDecisionAt.values.all {
                it >= recovered.engine.simulationTime
            }
        )
        check(fixture.repository.envelope?.gameState() == recovered)

        fixture.controller.advance(GameDuration.ofMillis(1L))

        check(fixture.controller.state.value.status == RuntimeHostStatus.READY)
        check(fixture.controller.state.value.failure == null)
        check(
            fixture.runtime.state().engine.simulationTime ==
                recovered.engine.simulationTime + GameDuration.ofMillis(1L)
        )
    }

    private fun fixture(): Fixture {
        val factory = SimulationTestSupport.factory()
        val setup = GameRuntime(
            initialSession = factory.newGame(4_002L),
            sessionFactory = factory
        )
        SimulationTestSupport.checkAccepted(setup.dispatch(SetHeroName("Aster")))
        SimulationTestSupport.startTraining(setup)

        val activeState = setup.state()
        val mixedState = activeState.copy(
            engine = activeState.engine.copy(
                simulationTime = activeState.engine.simulationTime + GameDuration.ofSeconds(60L)
            )
        )
        check(
            mixedState.run.combat.nextPlayerDecisionAt!! <
                mixedState.engine.simulationTime
        )

        val clock = SimulationTestSupport.MutableClock(5_000L)
        val repository = ToggleSaveRepository(
            SaveEnvelope.create(
                gameState = mixedState,
                contentVersion = SimulationTestSupport.CONTENT_VERSION,
                writtenAtEpochMs = clock.epochMs,
                schemaVersion = SaveVersion.CURRENT
            )
        )
        val runtime = GameRuntime(
            initialSession = factory.loadedGame(mixedState),
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
        val controller = GameRuntimeController(
            runtime = runtime,
            repository = repository,
            executor = InlineExecutor()
        )
        return Fixture(
            repository = repository,
            runtime = runtime,
            controller = controller,
            mixedState = mixedState
        )
    }

    private data class Fixture(
        val repository: ToggleSaveRepository,
        val runtime: GameRuntime,
        val controller: GameRuntimeController,
        val mixedState: GameState
    )

    private class ToggleSaveRepository(
        initial: SaveEnvelope
    ) : GameRepository {
        var envelope: SaveEnvelope? = initial
        var failSaves: Boolean = false

        override fun load(): SaveEnvelope? = envelope

        override fun save(envelope: SaveEnvelope) {
            check(!failSaves) { "test save failure" }
            this.envelope = envelope
        }

        override fun exists(): Boolean = envelope != null

        override fun delete() {
            envelope = null
        }
    }

    private class InlineExecutor : AbstractExecutorService() {
        private var shutdown = false

        override fun shutdown() {
            shutdown = true
        }

        override fun shutdownNow(): MutableList<Runnable> {
            shutdown = true
            return mutableListOf()
        }

        override fun isShutdown(): Boolean = shutdown

        override fun isTerminated(): Boolean = shutdown

        override fun awaitTermination(timeout: Long, unit: TimeUnit): Boolean = true

        override fun execute(command: Runnable) {
            check(!shutdown) { "InlineExecutor is shut down" }
            command.run()
        }
    }
}
