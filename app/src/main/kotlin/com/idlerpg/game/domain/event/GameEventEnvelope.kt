package com.idlerpg.game.domain.event

import com.idlerpg.game.core.time.GameTime
import com.idlerpg.game.domain.command.CommandCorrelationId

/**
 * Deterministic metadata wrapper for one completed domain event.
 *
 * Ordering is canonical by [sequenceNumber]. The simulation timestamp is domain time,
 * never wall-clock epoch time.
 */
data class GameEventEnvelope(
    val sequenceNumber: Long,
    val simulationTime: GameTime,
    val event: GameEvent,
    val commandCorrelationId: CommandCorrelationId? = null
) {
    init {
        require(sequenceNumber > 0L) {
            "GameEventEnvelope.sequenceNumber must be positive: $sequenceNumber"
        }
    }
}
