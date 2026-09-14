package com.idlerpg.game.core.random

/**
 * Serializable conceptual state for the deterministic PRNG.
 *
 * algorithmVersion is part of save compatibility. A future PRNG algorithm change must
 * use a different version and an explicit migration/version strategy.
 */
data class RandomState(
    val state: Long,
    val algorithmVersion: Int = CURRENT_ALGORITHM_VERSION
) {
    init {
        require(algorithmVersion > 0) {
            "Random algorithm version must be positive: $algorithmVersion"
        }
    }

    companion object {
        const val CURRENT_ALGORITHM_VERSION: Int = 1
    }
}
