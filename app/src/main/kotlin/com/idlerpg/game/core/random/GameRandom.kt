package com.idlerpg.game.core.random

/** Weighted value used by deterministic weighted selection. */
data class WeightedValue<T>(
    val value: T,
    val weight: Long
) {
    init {
        require(weight >= 0L) { "Weight cannot be negative: $weight" }
    }
}

/**
 * Deterministic gameplay randomness contract.
 *
 * Domain systems receive this abstraction through the future EngineContext. They must
 * never call uncontrolled random APIs directly.
 */
interface GameRandom {
    fun nextInt(bound: Int): Int

    fun nextLong(bound: Long): Long

    fun nextUnitDouble(): Double

    fun <T> chooseWeighted(options: List<WeightedValue<T>>): T

    fun snapshot(): RandomState
}
