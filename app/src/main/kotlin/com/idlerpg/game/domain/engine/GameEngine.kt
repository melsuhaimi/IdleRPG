package com.idlerpg.game.domain.engine

import com.idlerpg.game.domain.command.CommandRejectionCode
import com.idlerpg.game.domain.command.CommandRejectionReason
import com.idlerpg.game.domain.command.GameCommand
import com.idlerpg.game.domain.event.GameEvent
import com.idlerpg.game.domain.model.GameState

/**
 * Handler extension point used by later domain systems to implement concrete commands.
 *
 * Foundation 4 defines routing/atomicity only. Foundation-specific gameplay systems
 * supply handlers later without moving rules into GameEngine.
 */
fun interface GameCommandHandler {
    fun handle(
        state: GameState,
        command: GameCommand,
        context: EngineContext
    ): CommandHandlingResult
}

/** Internal pre-commit outcome returned by a concrete command handler. */
sealed interface CommandHandlingResult {

    /**
     * Proposed atomic transition. The handler may change RunState/MetaState but must not
     * directly change EngineState; engine metadata is committed centrally.
     */
    data class Accepted(
        val state: GameState,
        val events: List<GameEvent> = emptyList()
    ) : CommandHandlingResult

    data class Rejected(
        val reason: CommandRejectionReason
    ) : CommandHandlingResult
}

/** Default until a later foundation registers an implementation for a command family. */
object RejectUnsupportedGameCommandHandler : GameCommandHandler {
    override fun handle(
        state: GameState,
        command: GameCommand,
        context: EngineContext
    ): CommandHandlingResult = CommandHandlingResult.Rejected(
        CommandRejectionReason(
            code = CommandRejectionCode.UNSUPPORTED
        )
    )
}

/**
 * Deterministic discrete-command engine.
 *
 * Responsibilities in Foundation 4:
 * - restore controlled RNG/ID resources from canonical EngineState,
 * - route immutable intent to one injected command handler,
 * - reject without partial canonical mutation,
 * - commit accepted transitions atomically,
 * - persist RNG/ID progress,
 * - assign deterministic event sequence numbers and simulation timestamps.
 *
 * Combat/economy/world formulas do not belong here.
 */
object GameEngine {

    fun handle(
        state: GameState,
        command: GameCommand,
        context: EngineContext
    ): EngineResult {
        context.beginExecution(state.engine)

        return when (
            val handling = context.commandHandler.handle(
                state = state,
                command = command,
                context = context
            )
        ) {
            is CommandHandlingResult.Rejected -> EngineResult(
                state = state,
                events = emptyList(),
                commandResult = CommandResult.Rejected(handling.reason)
            )

            is CommandHandlingResult.Accepted -> {
                require(handling.state.engine == state.engine) {
                    "Command handlers must not mutate EngineState directly"
                }

                val commit = EngineEventCommitter.commit(
                    state = handling.state,
                    rawEvents = handling.events,
                    context = context,
                    commandCorrelationId = command.correlationId
                )

                EngineResult(
                    state = commit.state,
                    events = commit.envelopes,
                    commandResult = CommandResult.Accepted
                )
            }
        }
    }
}
