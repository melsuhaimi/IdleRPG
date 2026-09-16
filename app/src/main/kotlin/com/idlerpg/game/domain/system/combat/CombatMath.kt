package com.idlerpg.game.domain.system.combat

import com.idlerpg.game.core.number.GameNumber
import com.idlerpg.game.core.number.Ratio
import com.idlerpg.game.core.time.GameDuration
import java.math.BigInteger

/** Central fixed-point combat arithmetic. Calculation order is explicit and platform stable. */
object CombatMath {
    const val CRITICAL_ROLL_BOUND: Long = Ratio.UNITS_PER_ONE
    val DEFAULT_ARMOR_SCALE: GameNumber = GameNumber.of(100L)
    val MINIMUM_ACTION_INTERVAL: GameDuration = GameDuration.ofMillis(250L)

    /** Diminishing mitigation: damage * armorScale / (armorScale + effectiveArmor). */
    fun mitigate(
        damage: GameNumber,
        armor: GameNumber,
        armorPenetration: GameNumber,
        armorScale: GameNumber = DEFAULT_ARMOR_SCALE
    ): GameNumber {
        require(armorScale > GameNumber.ZERO) { "armorScale must be positive" }
        if (damage == GameNumber.ZERO) return GameNumber.ZERO
        val effectiveArmor = if (armorPenetration >= armor) GameNumber.ZERO else armor - armorPenetration
        val numerator = damage.toBigInteger().multiply(armorScale.toBigInteger())
        val denominator = armorScale.toBigInteger().add(effectiveArmor.toBigInteger())
        val mitigated = GameNumber.fromBigInteger(numerator.divide(denominator))
        return if (mitigated < GameNumber.ONE) GameNumber.ONE else mitigated
    }

    /**
     * Expected damage of a hit when critical chance and critical multiplier are included.
     *
     * [criticalMultiplier] is the total critical-hit multiplier (2.0 means double damage),
     * while [criticalChance] is capped at 100%. The expectation is a readout formula only;
     * actual combat still consumes one canonical RNG roll per eligible hit.
     */
    fun expectedCriticalDamage(
        baseDamage: GameNumber,
        criticalChance: Ratio,
        criticalMultiplier: Ratio
    ): GameNumber {
        if (baseDamage == GameNumber.ZERO) return GameNumber.ZERO
        val chanceUnits = criticalChance.units.coerceAtMost(Ratio.UNITS_PER_ONE)
        val bonusUnits = (criticalMultiplier.units - Ratio.UNITS_PER_ONE).coerceAtLeast(0L)
        val one = BigInteger.valueOf(Ratio.UNITS_PER_ONE)
        val denominator = one.multiply(one)
        val expectedMultiplierNumerator = denominator.add(
            BigInteger.valueOf(chanceUnits).multiply(BigInteger.valueOf(bonusUnits))
        )
        return GameNumber.fromBigInteger(
            baseDamage.toBigInteger()
                .multiply(expectedMultiplierNumerator)
                .divide(denominator)
        )
    }

    fun actionInterval(
        base: GameDuration,
        actionSpeed: Ratio,
        minimum: GameDuration = MINIMUM_ACTION_INTERVAL
    ): GameDuration {
        require(base > GameDuration.ZERO) { "base action interval must be positive" }
        require(actionSpeed > Ratio.ZERO) { "actionSpeed must be positive" }
        val scaledMillis = BigInteger.valueOf(base.millis)
            .multiply(BigInteger.valueOf(Ratio.UNITS_PER_ONE))
            .divide(BigInteger.valueOf(actionSpeed.units))
            .coerceAtLeast(BigInteger.ONE)
        val bounded = scaledMillis.min(BigInteger.valueOf(Long.MAX_VALUE)).toLong()
        val interval = GameDuration.ofMillis(bounded)
        return if (interval < minimum) minimum else interval
    }

    fun isCritical(rollUnits: Long, criticalChance: Ratio): Boolean {
        require(rollUnits in 0L until CRITICAL_ROLL_BOUND) { "critical roll is out of range" }
        val boundedChance = criticalChance.units.coerceAtMost(Ratio.UNITS_PER_ONE)
        return rollUnits < boundedChance
    }
}
