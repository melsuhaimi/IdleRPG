package com.idlerpg.game.application

import com.idlerpg.game.core.time.GameDuration
import com.idlerpg.game.data.local.SaveEnvelope
import com.idlerpg.game.domain.command.GameCommand
import com.idlerpg.game.domain.engine.EngineResult
import com.idlerpg.game.domain.event.GameEventEnvelope
import com.idlerpg.game.domain.model.GameState

/**
 * Top-level headless backend facade.
 *
 * GameRuntime is now the application-level owner of the active GameSession promised by
 * the backend-first architecture. It coordinates command dispatch, foreground simulation,
 * explicit save requests, and optional offline-resume replacement. It contains no combat,
 * economy, loot, Resonance, Doctrine, Adaptation, or Chronicle formulas.
 */
class GameRuntime(
    initialSession: GameSession,
    private val sessionFactory: GameSessionFactory,
    private val autosaveCoordinator: AutosaveCoordinator? = null,
    private val offlineSessionCoordinator: OfflineSessionCoordinator? = null
) {
    private var activeSession: GameSession = initialSession

    private val snapshots = GameSnapshotProvider {
        activeSession
    }

    fun state(): GameState = activeSession.state()

    fun snapshot(): GameState = snapshots.snapshot()

    fun recentEvents(): List<GameEventEnvelope> =
        activeSession.recentEvents()

    fun dispatch(command: GameCommand): EngineResult =
        activeSession.applyCommand(command) { result ->
            if (result.commandResult == com.idlerpg.game.domain.engine.CommandResult.Accepted) {
                autosaveCoordinator?.save(result.state)
            }
        }

    fun advance(duration: GameDuration): EngineResult =
        activeSession.advance(duration) { result ->
            if (result.events.any { it.event is com.idlerpg.game.domain.event.ItemDropped }) {
                autosaveCoordinator?.save(result.state)
            }
        }

    fun startNewGame(
        randomSeed: Long = com.idlerpg.game.domain.model.EngineState.DEFAULT_RANDOM_SEED
    ): GameState {
        activeSession = sessionFactory.newPlayableGame(randomSeed)
        return activeSession.state()
    }

    fun replaceLoadedState(state: GameState): GameState {
        activeSession = sessionFactory.loadedGame(state)
        return activeSession.state()
    }

    /**
     * Explicit save checkpoint.
     *
     * A runtime can be constructed without persistence; in that case this operation fails
     * clearly instead of silently pretending a save occurred.
     */
    fun saveNow(): SaveEnvelope {
        val coordinator = autosaveCoordinator
            ?: error("GameRuntime has no AutosaveCoordinator")
        return coordinator.save(activeSession)
    }

    /**
     * Resume through Foundation 16's canonical offline coordinator and make its resulting
     * state the active foreground session.
     */
    fun resumeOffline(): OfflineResumeResult? {
        val coordinator = offlineSessionCoordinator
            ?: error("GameRuntime has no OfflineSessionCoordinator")
        val result = coordinator.resume() ?: return null
        activeSession = sessionFactory.loadedGame(result.engineResult.state)
        return result
    }
}
