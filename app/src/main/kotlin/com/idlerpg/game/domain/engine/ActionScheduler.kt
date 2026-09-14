package com.idlerpg.game.domain.engine

import com.idlerpg.game.core.time.GameTime

/**
 * Stateless deterministic scheduler selector.
 *
 * The scheduler does not own a mutable queue. Later gameplay systems project scheduled
 * actions from canonical GameState, preserving the architecture law that resumable
 * gameplay facts live in state rather than an ephemeral engine-only queue.
 */
object ActionScheduler {

    private val comparator: Comparator<ScheduledAction> =
        compareBy<ScheduledAction>(
            { it.dueAt },
            { it.priority.order },
            { it.stableTieBreakKey }
        )

    /** Return every action in canonical deterministic execution order. */
    fun ordered(actions: List<ScheduledAction>): List<ScheduledAction> {
        validateUniqueOrderingKeys(actions)
        return actions.sortedWith(comparator)
    }

    /**
     * Select the next action that is due between [currentTime] and [targetTime], inclusive.
     *
     * A projected action in the past is an internal scheduler/state error and fails fast.
     */
    fun nextDueAction(
        actions: List<ScheduledAction>,
        currentTime: GameTime,
        targetTime: GameTime
    ): ScheduledAction? {
        require(targetTime >= currentTime) {
            "targetTime cannot precede currentTime: $targetTime < $currentTime"
        }

        validateUniqueOrderingKeys(actions)

        for (action in actions) {
            require(action.dueAt >= currentTime) {
                "Scheduled action is in the past: ${action.dueAt} < $currentTime"
            }
        }

        return actions
            .asSequence()
            .filter { it.dueAt <= targetTime }
            .minWithOrNull(comparator)
    }

    private fun validateUniqueOrderingKeys(actions: List<ScheduledAction>) {
        val seen = HashSet<OrderingKey>(actions.size)

        for (action in actions) {
            val key = OrderingKey(
                dueAt = action.dueAt,
                priorityOrder = action.priority.order,
                stableTieBreakKey = action.stableTieBreakKey
            )

            require(seen.add(key)) {
                "Ambiguous scheduled actions share the same dueAt/priority/tie-break key: $key"
            }
        }
    }

    private data class OrderingKey(
        val dueAt: GameTime,
        val priorityOrder: Int,
        val stableTieBreakKey: Long
    )
}
