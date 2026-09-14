package com.idlerpg.game.core.number

import java.math.BigInteger

/**
 * Canonical exact non-negative gameplay magnitude.
 *
 * Backed by BigInteger so idle-RPG progression is not limited by Long.MAX_VALUE.
 */
class GameNumber private constructor(
    private val value: BigInteger
) : Comparable<GameNumber> {

    init {
        require(value.signum() >= 0) {
            "GameNumber cannot be negative: $value"
        }
    }

    operator fun plus(other: GameNumber): GameNumber =
        fromBigInteger(value.add(other.value))

    operator fun minus(other: GameNumber): GameNumber {
        require(value >= other.value) {
            "GameNumber subtraction would underflow: $value - ${other.value}"
        }
        return fromBigInteger(value.subtract(other.value))
    }

    operator fun times(multiplier: Long): GameNumber {
        require(multiplier >= 0L) {
            "GameNumber multiplier cannot be negative: $multiplier"
        }
        return fromBigInteger(value.multiply(BigInteger.valueOf(multiplier)))
    }

    operator fun times(other: GameNumber): GameNumber =
        fromBigInteger(value.multiply(other.value))

    fun divide(divisor: Long): GameNumber {
        require(divisor > 0L) { "GameNumber divisor must be > 0: $divisor" }
        return fromBigInteger(value.divide(BigInteger.valueOf(divisor)))
    }

    fun divide(divisor: GameNumber): GameNumber {
        require(!divisor.isZero()) { "GameNumber divisor must be > 0" }
        return fromBigInteger(value.divide(divisor.value))
    }

    fun isZero(): Boolean = value.signum() == 0

    fun toBigInteger(): BigInteger = value

    fun toPlainString(): String = value.toString()

    override fun compareTo(other: GameNumber): Int = value.compareTo(other.value)

    override fun equals(other: Any?): Boolean =
        other is GameNumber && value == other.value

    override fun hashCode(): Int = value.hashCode()

    override fun toString(): String = value.toString()

    companion object {
        val ZERO: GameNumber = GameNumber(BigInteger.ZERO)
        val ONE: GameNumber = GameNumber(BigInteger.ONE)

        fun of(value: Long): GameNumber {
            require(value >= 0L) { "GameNumber cannot be negative: $value" }
            return GameNumber(BigInteger.valueOf(value))
        }

        fun parse(value: String): GameNumber = fromBigInteger(BigInteger(value))

        fun fromBigInteger(value: BigInteger): GameNumber = GameNumber(value)
    }
}
