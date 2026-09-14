package com.idlerpg.game.domain.engine

import com.idlerpg.game.core.time.GameDuration
import com.idlerpg.game.domain.event.GameEvent
import com.idlerpg.game.domain.model.GameState

/**
 * Projects autonomous work from canonical GameState.
 *
 * Implementations must be pure projections: they must not consume RNG, allocate IDs, or
 * mutate state while merely asking what is scheduled next.
 */
fun interface ScheduledActionSource {
    fun scheduledActions(state: GameState): List<ScheduledAction>
}

/** Default source before gameplay systems introduce autonomous scheduled work. */
object EmptyScheduledActionSource : ScheduledActionSource {
    override fun scheduledActions(state: GameState): List<ScheduledAction> = emptyList()
}

/** Complete result of executing one selected scheduled action before engine commit. */
data class ScheduledActionExecution(
    val state: GameState,
    val events: List<GameEvent> = emptyList()
)

/** Handler extension point implemented by later combat/world/status systems. */
fun interface ScheduledActionHandler {
    fun execute(
        state: GameState,
        action: ScheduledAction,
        context: EngineContext
    ): ScheduledActionExecution
}

/** Fails fast if an action exists but no later-foundation handler has been registered. */
object RejectUnexpectedScheduledActionHandler : ScheduledActionHandler {
    override fun execute(
        state: GameState,
        action: ScheduledAction,
        context: EngineContext
    ): ScheduledActionExecution = error(
        "No ScheduledActionHandler is registered for ${action.type}"
    )
}

/**
 * Raised when a single advance would exceed the configured safety ceiling.
 *
 * Partial state is not returned to the caller. A future optimized offline path may avoid
 * the ceiling only after equivalence with the canonical engine is tested.
 */
class SimulationActionLimitExceededException(
    maximumActions: Int
) : IllegalStateException(
    "Simulation exceeded maximumActionsPerAdvance=$maximumActions"
)

/**
 * Deterministic event-driven simulation engine.
 *
 * The engine jumps directly between meaningful scheduled times. It does not run a
 * rendering-frame tick and does not read wall-clock time.
 */
object SimulationEngine {

    fun advance(
        state: GameState,
        duration: GameDuration,
        context: EngineContext
    ): EngineResult {
        context.beginExecution(state.engine)

        val targetTime = state.engine.simulationTime + duration
        var currentState = state
        val emittedEvents = mutableListOf<com.idlerpg.game.domain.event.GameEventEnvelope>()
        var processedActions = 0

        while (true) {
            val projectedActions = context.scheduledActionSource.scheduledActions(currentState)
            val nextAction = ActionScheduler.nextDueAction(
                actions = projectedActions,
                currentTime = currentState.engine.simulationTime,
                targetTime = targetTime
            )

            if (nextAction == null) {
                val atTarget = currentState.copy(
                    engine = currentState.engine.copy(
                        simulationTime = targetTime
                    )
                )

                val finalCommit = EngineEventCommitter.commit(
                    state = atTarget,
                    rawEvents = emptyList(),
                    context = context
                )

                return EngineResult(
                    state = finalCommit.state,
                    events = emittedEvents.toList(),
                    commandResult = null,
                    diagnostics = EngineDiagnostics(
                        processedScheduledActions = processedActions
                    )
                )
            }

            if (processedActions >= context.simulationConfig.maximumActionsPerAdvance) {
                throw SimulationActionLimitExceededException(
                    maximumActions = context.simulationConfig.maximumActionsPerAdvance
                )
            }

            val stateAtActionTime = currentState.copy(
                engine = currentState.engine.copy(
                    simulationTime = nextAction.dueAt
                )
            )

            val execution = context.scheduledActionHandler.execute(
                state = stateAtActionTime,
                action = nextAction,
                context = context
            )

            require(execution.state.engine == stateAtActionTime.engine) {
                "ScheduledActionHandler must not mutate EngineState directly"
            }

            val commit = EngineEventCommitter.commit(
                state = execution.state,
                rawEvents = execution.events,
                context = context
            )

            currentState = commit.state
            emittedEvents += commit.envelopes
            processedActions += 1
        }
    }
}
