package com.idlerpg.game.core.number

import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Deterministic non-negative fractional gameplay rate, such as DPS or Gold/second.
 *
 * Rates use one fixed decimal scale and DOWN rounding. Rates are derived values and are
 * not intended to become canonical mutable gameplay state.
 */
class GameRate private constructor(
    private val value: BigDecimal
) : Comparable<GameRate> {

    init {
        require(value.signum() >= 0) {
            "GameRate cannot be negative: $value"
        }
    }

    operator fun plus(other: GameRate): GameRate =
        fromBigDecimal(value.add(other.value))

    fun toBigDecimal(): BigDecimal = value

    fun toPlainString(): String = value.stripTrailingZeros().toPlainString()

    override fun compareTo(other: GameRate): Int = value.compareTo(other.value)

    override fun equals(other: Any?): Boolean =
        other is GameRate && value.compareTo(other.value) == 0

    override fun hashCode(): Int = value.stripTrailingZeros().hashCode()

    override fun toString(): String = toPlainString()

    companion object {
        const val SCALE: Int = 6
        val ZERO: GameRate = GameRate(BigDecimal.ZERO.setScale(SCALE))

        fun of(value: Long): GameRate {
            require(value >= 0L) { "GameRate cannot be negative: $value" }
            return fromBigDecimal(BigDecimal.valueOf(value))
        }

        fun parse(value: String): GameRate = fromBigDecimal(BigDecimal(value))

        fun fromBigDecimal(value: BigDecimal): GameRate = GameRate(
            value.setScale(SCALE, RoundingMode.DOWN)
        )
    }
}
