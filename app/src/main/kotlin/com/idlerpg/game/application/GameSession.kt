package com.idlerpg.game.application

import com.idlerpg.game.core.time.GameDuration
import com.idlerpg.game.domain.command.GameCommand
import com.idlerpg.game.domain.engine.EngineContext
import com.idlerpg.game.domain.engine.EngineResult
import com.idlerpg.game.domain.engine.SimulationEngine
import com.idlerpg.game.domain.event.GameEventEnvelope
import com.idlerpg.game.domain.model.GameState

/**
 * One loaded, headless playable game session.
 *
 * The session owns exactly one current immutable GameState reference. Every accepted
 * command or simulation advancement replaces that reference with the returned canonical
 * state. Only the most recent transition's events are retained here, preventing an
 * unbounded event log from becoming canonical session state.
 *
 * Calls are intentionally expected to be serialized by the application owner. Domain
 * determinism must never depend on thread scheduling.
 */
class GameSession internal constructor(
    initialState: GameState,
    private val engineContext: EngineContext
) {
    private val commandDispatcher = CommandDispatcher(engineContext)

    private var currentState: GameState = initialState
    private var currentRecentEvents: List<GameEventEnvelope> = emptyList()

    fun state(): GameState = currentState

    fun recentEvents(): List<GameEventEnvelope> = currentRecentEvents

    fun applyCommand(command: GameCommand): EngineResult {
        val result = commandDispatcher.dispatch(
            state = currentState,
            command = command
        )
        commit(result)
        return result
    }

    fun advance(duration: GameDuration): EngineResult {
        val result = SimulationEngine.advance(
            state = currentState,
            duration = duration,
            context = engineContext
        )
        commit(result)
        return result
    }

    /**
     * Replace the session state with an explicitly loaded/debug state.
     *
     * No events are synthesized because loading is an application lifecycle action, not a
     * completed gameplay fact.
     */
    fun replaceLoadedState(state: GameState) {
        currentState = state
        currentRecentEvents = emptyList()
    }

    internal fun context(): EngineContext = engineContext

    private fun commit(result: EngineResult) {
        currentState = result.state
        currentRecentEvents = result.events
    }
}
