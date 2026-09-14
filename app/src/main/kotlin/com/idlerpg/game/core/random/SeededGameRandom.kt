package com.idlerpg.game.core.random

/**
 * Deterministic SplitMix64-based PRNG.
 *
 * Algorithm version 1 is now an explicit compatibility contract. Do not change the
 * constants or output transformation after persistent saves depend on this generator
 * without introducing a new RandomState.algorithmVersion strategy.
 */
class SeededGameRandom(
    seed: Long
) : GameRandom {

    private var internalState: Long = seed

    constructor(state: RandomState) : this(seed = state.state) {
        require(state.algorithmVersion == RandomState.CURRENT_ALGORITHM_VERSION) {
            "Unsupported random algorithm version: ${state.algorithmVersion}"
        }
    }

    override fun nextInt(bound: Int): Int {
        require(bound > 0) { "bound must be > 0: $bound" }
        return nextLong(bound.toLong()).toInt()
    }

    override fun nextLong(bound: Long): Long {
        require(bound > 0L) { "bound must be > 0: $bound" }

        var bits = nextRawLong().ushr(1)
        val mask = bound - 1L

        if ((bound and mask) == 0L) {
            return bits and mask
        }

        var value = bits % bound
        while (bits - value + mask < 0L) {
            bits = nextRawLong().ushr(1)
            value = bits % bound
        }

        return value
    }

    override fun nextUnitDouble(): Double {
        val precisionBits = nextRawLong().ushr(11)
        return precisionBits.toDouble() * DOUBLE_UNIT
    }

    override fun <T> chooseWeighted(options: List<WeightedValue<T>>): T {
        require(options.isNotEmpty()) { "Weighted selection requires at least one option" }

        var totalWeight = 0L
        for (option in options) {
            totalWeight = Math.addExact(totalWeight, option.weight)
        }

        require(totalWeight > 0L) {
            "Weighted selection requires at least one positive weight"
        }

        val roll = nextLong(totalWeight)
        var cursor = 0L

        for (option in options) {
            cursor = Math.addExact(cursor, option.weight)
            if (roll < cursor) {
                return option.value
            }
        }

        error("Weighted selection reached an impossible state")
    }

    override fun snapshot(): RandomState = RandomState(
        state = internalState,
        algorithmVersion = RandomState.CURRENT_ALGORITHM_VERSION
    )

    private fun nextRawLong(): Long {
        internalState += GOLDEN_GAMMA

        var value = internalState
        value = (value xor (value ushr 30)) * MIX_MULTIPLIER_1
        value = (value xor (value ushr 27)) * MIX_MULTIPLIER_2
        return value xor (value ushr 31)
    }

    private companion object {
        // Signed Long forms of the canonical SplitMix64 constants.
        const val GOLDEN_GAMMA: Long = -7046029254386353131L
        const val MIX_MULTIPLIER_1: Long = -4658895280553007687L
        const val MIX_MULTIPLIER_2: Long = -7723592293110705685L
        const val DOUBLE_UNIT: Double = 1.0 / 9_007_199_254_740_992.0 // 2^53
    }
}
