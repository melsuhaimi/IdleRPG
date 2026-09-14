package com.idlerpg.game.core.id

/**
 * Controlled monotonic instance-ID source.
 *
 * The future EngineState owns the persisted next-ID counter. A session creates this
 * factory from that counter and snapshots [nextValue] back into canonical engine state.
 */
class IdFactory(
    initialNextValue: Long = 1L
) {
    private var nextCounter: Long = initialNextValue

    init {
        require(initialNextValue > 0L) {
            "Initial instance ID counter must be positive: $initialNextValue"
        }
    }

    fun next(): InstanceId {
        if (nextCounter == Long.MAX_VALUE) {
            throw IllegalStateException("InstanceId counter exhausted Long range")
        }

        val id = InstanceId(nextCounter)
        nextCounter += 1L
        return id
    }

    fun nextValue(): Long = nextCounter
}
