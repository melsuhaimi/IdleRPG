package com.idlerpg.game.core.config

/** Engine execution-policy configuration. */
data class SimulationConfig(
    val maximumActionsPerAdvance: Int = DEFAULT_MAXIMUM_ACTIONS_PER_ADVANCE,
    val schedulerOrderingVersion: Int = DEFAULT_SCHEDULER_ORDERING_VERSION
) {
    init {
        require(maximumActionsPerAdvance > 0) {
            "maximumActionsPerAdvance must be positive: $maximumActionsPerAdvance"
        }
        require(schedulerOrderingVersion > 0) {
            "schedulerOrderingVersion must be positive: $schedulerOrderingVersion"
        }
    }

    companion object {
        // Safety ceiling only; it is not a gameplay balance number.
        const val DEFAULT_MAXIMUM_ACTIONS_PER_ADVANCE: Int = 100_000
        const val DEFAULT_SCHEDULER_ORDERING_VERSION: Int = 1
    }
}
