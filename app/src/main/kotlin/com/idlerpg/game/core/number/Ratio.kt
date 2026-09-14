package com.idlerpg.game.core.number

/**
 * Deterministic non-negative fixed-point multiplier.
 *
 * 10,000 units represent 100.00%.
 * 5,000 units represent 50.00%.
 * 12,500 units represent 125.00%.
 */
data class Ratio private constructor(
    val units: Long
) : Comparable<Ratio> {

    init {
        require(units >= 0L) { "Ratio units cannot be negative: $units" }
    }

    override fun compareTo(other: Ratio): Int = units.compareTo(other.units)

    companion object {
        const val UNITS_PER_ONE: Long = 10_000L

        val ZERO: Ratio = Ratio(0L)
        val ONE: Ratio = Ratio(UNITS_PER_ONE)
        val HALF: Ratio = Ratio(UNITS_PER_ONE / 2L)

        fun ofUnits(units: Long): Ratio = Ratio(units)
    }
}
