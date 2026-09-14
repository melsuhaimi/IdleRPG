package com.idlerpg.game.domain.model

import com.idlerpg.game.core.random.RandomState
import com.idlerpg.game.core.time.GameTime

/**
 * Deterministic engine mechanics that must survive save/load.
 *
 * This state intentionally contains no wall-clock timestamp. Wall time belongs to the
 * future persistence/application boundary, while simulation time belongs here.
 */
data class EngineState(
    val simulationTime: GameTime = GameTime.ZERO,
    val randomState: RandomState = RandomState(DEFAULT_RANDOM_SEED),
    val nextEventSequenceNumber: Long = FIRST_EVENT_SEQUENCE_NUMBER,
    val nextInstanceIdCounter: Long = FIRST_INSTANCE_ID_COUNTER
) {
    init {
        require(nextEventSequenceNumber > 0L) {
            "nextEventSequenceNumber must be positive: $nextEventSequenceNumber"
        }
        require(nextInstanceIdCounter > 0L) {
            "nextInstanceIdCounter must be positive: $nextInstanceIdCounter"
        }
    }

    companion object {
        const val DEFAULT_RANDOM_SEED: Long = 0L
        const val FIRST_EVENT_SEQUENCE_NUMBER: Long = 1L
        const val FIRST_INSTANCE_ID_COUNTER: Long = 1L
    }
}
