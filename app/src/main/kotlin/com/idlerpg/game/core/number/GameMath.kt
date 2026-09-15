package com.idlerpg.game.core.number

import java.math.BigInteger

/** Central deterministic helpers for authoritative gameplay arithmetic. */
object GameMath {

    /**
     * Applies [ratio] to [value] using the project default authoritative rounding policy:
     * floor after the final multiplier application.
     */
    fun applyRatio(value: GameNumber, ratio: Ratio): GameNumber {
        val numerator = value.toBigInteger().multiply(BigInteger.valueOf(ratio.units))
        val result = numerator.divide(BigInteger.valueOf(Ratio.UNITS_PER_ONE))
        return GameNumber.fromBigInteger(result)
    }

    /**
     * Applies a fixed-point multiplier repeatedly using exact rational arithmetic.
     *
     * The result is floored after the final multiplication. Exponentiation by squaring keeps
     * the operation logarithmic in [steps] and avoids floating-point drift.
     */
    fun compound(
        value: GameNumber,
        multiplier: Ratio,
        steps: Long
    ): GameNumber {
        require(steps >= 0L) { "steps cannot be negative: $steps" }
        val numerator = value.toBigInteger().multiply(
            exactPower(BigInteger.valueOf(multiplier.units), steps)
        )
        val denominator = exactPower(
            BigInteger.valueOf(Ratio.UNITS_PER_ONE),
            steps
        )
        return GameNumber.fromBigInteger(numerator.divide(denominator))
    }

    /** Applies a fixed-point multiplier repeatedly and rounds the final result upward. */
    fun compoundCeil(
        value: GameNumber,
        multiplier: Ratio,
        steps: Long
    ): GameNumber {
        require(steps >= 0L) { "steps cannot be negative: $steps" }
        val numerator = value.toBigInteger().multiply(
            exactPower(BigInteger.valueOf(multiplier.units), steps)
        )
        val denominator = exactPower(
            BigInteger.valueOf(Ratio.UNITS_PER_ONE),
            steps
        )
        val (quotient, remainder) = numerator.divideAndRemainder(denominator)
        return GameNumber.fromBigInteger(
            if (remainder.signum() == 0) quotient else quotient.add(BigInteger.ONE)
        )
    }

    /**
     * Applies two fixed-point compounding segments and rounds only once at the end.
     *
     * Keeping both segments rational until the final division is important at the level soft cap:
     * rounding the first segment early would make the 800-to-801 transition drift.
     */
    fun compoundCeil(
        value: GameNumber,
        firstMultiplier: Ratio,
        firstSteps: Long,
        secondMultiplier: Ratio,
        secondSteps: Long
    ): GameNumber {
        require(firstSteps >= 0L) { "firstSteps cannot be negative: $firstSteps" }
        require(secondSteps >= 0L) { "secondSteps cannot be negative: $secondSteps" }
        val numerator = value.toBigInteger()
            .multiply(exactPower(BigInteger.valueOf(firstMultiplier.units), firstSteps))
            .multiply(exactPower(BigInteger.valueOf(secondMultiplier.units), secondSteps))
        val denominator = exactPower(
            BigInteger.valueOf(Ratio.UNITS_PER_ONE),
            Math.addExact(firstSteps, secondSteps)
        )
        val (quotient, remainder) = numerator.divideAndRemainder(denominator)
        return GameNumber.fromBigInteger(
            if (remainder.signum() == 0) quotient else quotient.add(BigInteger.ONE)
        )
    }

    /** Exact exponentiation for non-negative [exponent] values. */
    private fun exactPower(base: BigInteger, exponent: Long): BigInteger {
        var remaining = exponent
        var factor = base
        var result = BigInteger.ONE
        while (remaining > 0L) {
            if (remaining % 2L == 1L) {
                result = result.multiply(factor)
            }
            factor = factor.multiply(factor)
            remaining /= 2L
        }
        return result
    }

    /**
     * Applies a deterministic per-step growth rate once for each logical step.
     *
     * This is intentionally not a loop. A tier-100 value is calculated as
     * `base * (1 + rate * 100)`, which avoids compounding runaway inflation while
     * remaining exact for [GameNumber] and safe for very large idle values.
     */
    fun scaleByStep(
        value: GameNumber,
        growthPerStep: Ratio,
        steps: Long
    ): GameNumber {
        require(steps >= 0L) { "steps cannot be negative: $steps" }
        val multiplierUnits = BigInteger.valueOf(Ratio.UNITS_PER_ONE)
            .add(BigInteger.valueOf(growthPerStep.units).multiply(BigInteger.valueOf(steps)))
        return GameNumber.fromBigInteger(
            value.toBigInteger()
                .multiply(multiplierUnits)
                .divide(BigInteger.valueOf(Ratio.UNITS_PER_ONE))
        )
    }

    /** Fixed-point ratio after additive per-step growth, clamped only at Long.MAX_VALUE. */
    fun ratioAfterSteps(
        base: Ratio,
        growthPerStep: Ratio,
        steps: Long
    ): Ratio {
        require(steps >= 0L) { "steps cannot be negative: $steps" }
        val units = BigInteger.valueOf(base.units)
            .add(BigInteger.valueOf(growthPerStep.units).multiply(BigInteger.valueOf(steps)))
            .min(BigInteger.valueOf(Long.MAX_VALUE))
            .longValueExact()
        return Ratio.ofUnits(units)
    }

    /** Multiplies fixed-point ratios without overflowing the Long intermediate. */
    fun multiplyRatios(left: Ratio, right: Ratio): Ratio {
        val units = BigInteger.valueOf(left.units)
            .multiply(BigInteger.valueOf(right.units))
            .divide(BigInteger.valueOf(Ratio.UNITS_PER_ONE))
            .min(BigInteger.valueOf(Long.MAX_VALUE))
            .longValueExact()
        return Ratio.ofUnits(units)
    }

    /** n * (n - 1) / 2, used by the progressive XP curve. */
    fun triangular(n: Long): BigInteger {
        require(n >= 0L) { "n cannot be negative: $n" }
        val value = BigInteger.valueOf(n)
        return value.multiply(value.subtract(BigInteger.ONE)).divide(BigInteger.TWO)
    }

    fun clamp(
        value: GameNumber,
        minimum: GameNumber,
        maximum: GameNumber
    ): GameNumber {
        require(minimum <= maximum) {
            "minimum cannot exceed maximum: $minimum > $maximum"
        }

        return when {
            value < minimum -> minimum
            value > maximum -> maximum
            else -> value
        }
    }

    /** Generic deterministic linear growth helper. Feature-specific meaning stays outside core. */
    fun linearGrowth(
        base: GameNumber,
        increment: GameNumber,
        level: Long
    ): GameNumber {
        require(level >= 0L) { "level cannot be negative: $level" }
        return base + (increment * level)
    }
}
