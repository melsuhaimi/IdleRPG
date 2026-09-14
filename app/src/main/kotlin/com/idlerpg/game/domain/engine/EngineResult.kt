package com.idlerpg.game.domain.engine

import com.idlerpg.game.domain.command.CommandCorrelationId
import com.idlerpg.game.domain.event.GameEvent
import com.idlerpg.game.domain.event.GameEventEnvelope
import com.idlerpg.game.domain.model.GameState
import com.idlerpg.game.domain.system.achievement.AchievementSystem
import com.idlerpg.game.domain.system.quest.QuestSystem
import java.util.ArrayDeque

/** Lightweight deterministic execution diagnostics. */
data class EngineDiagnostics(
    val processedScheduledActions: Int = 0
) {
    init {
        require(processedScheduledActions >= 0) {
            "processedScheduledActions cannot be negative: $processedScheduledActions"
        }
    }
}

/**
 * Atomic output of a command or simulation advancement.
 *
 * [state] is the new canonical state. [events] are ordered completed facts emitted by
 * this execution only. Event history is intentionally not accumulated in GameState.
 */
data class EngineResult(
    val state: GameState,
    val events: List<GameEventEnvelope> = emptyList(),
    val commandResult: CommandResult? = null,
    val diagnostics: EngineDiagnostics = EngineDiagnostics()
)

/** Internal event commit result shared by GameEngine and SimulationEngine. */
internal data class EventCommit(
    val state: GameState,
    val envelopes: List<GameEventEnvelope>
)

internal data class EventReactionCommit(
    val state: GameState,
    val events: List<GameEvent>
)

/**
 * Commits controlled RNG/ID progress, applies deterministic event-driven objective
 * reactions, and assigns canonical event sequence metadata.
 */
internal object EngineEventCommitter {

    fun commit(
        state: GameState,
        rawEvents: List<GameEvent>,
        context: EngineContext,
        commandCorrelationId: CommandCorrelationId? = null
    ): EventCommit {
        val reactions = applyEventReactions(
            state = state,
            rawEvents = rawEvents,
            context = context
        )
        var nextSequenceNumber = reactions.state.engine.nextEventSequenceNumber

        val envelopes = reactions.events.map { event ->
            val sequenceNumber = nextSequenceNumber
            nextSequenceNumber = Math.addExact(nextSequenceNumber, 1L)

            GameEventEnvelope(
                sequenceNumber = sequenceNumber,
                simulationTime = reactions.state.engine.simulationTime,
                event = event,
                commandCorrelationId = commandCorrelationId
            )
        }

        val committedEngineState = context.snapshotEngineResources(
            base = reactions.state.engine
        ).copy(
            nextEventSequenceNumber = nextSequenceNumber
        )

        return EventCommit(
            state = reactions.state.copy(engine = committedEngineState),
            envelopes = envelopes
        )
    }

    /**
     * Foundation 14 event reaction ordering:
     *
     * 1. preserve the producing system's raw event order;
     * 2. for each completed event, update run quests first;
     * 3. update persistent achievements from the same completed event;
     * 4. process reward-generated events immediately after their causal event.
     *
     * Quest/achievement definitions are stable-ID ordered internally. Foundation 14
     * validates repeatable quests as unsupported, so completion-driven reward reactions
     * are bounded by the finite authored objective set.
     */
    private fun applyEventReactions(
        state: GameState,
        rawEvents: List<GameEvent>,
        context: EngineContext
    ): EventReactionCommit {
        var transitioned = state
        val pending = ArrayDeque<GameEvent>()
        rawEvents.forEach(pending::addLast)
        val ordered = mutableListOf<GameEvent>()

        while (pending.isNotEmpty()) {
            val event = pending.removeFirst()
            ordered += event

            val quest = QuestSystem.react(
                state = transitioned,
                event = event,
                context = context
            )
            transitioned = quest.state

            val achievement = AchievementSystem.react(
                state = transitioned,
                event = event,
                context = context
            )
            transitioned = achievement.state

            val generated = quest.events + achievement.events
            for (index in generated.indices.reversed()) {
                pending.addFirst(generated[index])
            }
        }

        return EventReactionCommit(
            state = transitioned,
            events = ordered
        )
    }
}
