package com.idlerpg.game.domain.system.combat

import com.idlerpg.game.core.number.GameMath
import com.idlerpg.game.core.number.GameNumber
import com.idlerpg.game.core.number.Ratio
import java.math.BigInteger

/** Chance-based Guard behavior kept separate from Armor's physical mitigation. */
object DefenseSystem {
    const val GUARD_CHANCE_SCALE: Long = 1_000L
    const val MAX_GUARD_CHANCE_UNITS: Long = 7_500L
    const val GUARD_BLOCK_RATIO_UNITS: Long = 5_000L

    data class GuardResult(val damage: GameNumber, val guarded: Boolean)

    /** Defense / (1,000 + Defense), capped at a 75% Guard chance. */
    fun guardChance(defense: GameNumber): Ratio {
        if (defense <= GameNumber.ZERO) return Ratio.ZERO
        val defenseValue = defense.toBigInteger()
        val denominator = defenseValue.add(BigInteger.valueOf(GUARD_CHANCE_SCALE))
        val units = defenseValue
            .multiply(BigInteger.valueOf(Ratio.UNITS_PER_ONE))
            .divide(denominator)
            .min(BigInteger.valueOf(MAX_GUARD_CHANCE_UNITS))
        return Ratio.ofUnits(units.longValueExact())
    }

    /** A successful Guard blocks half of post-Armor damage for every damage kind. */
    fun resolve(damage: GameNumber, defense: GameNumber, rollUnits: Long): GuardResult {
        require(rollUnits in 0L until Ratio.UNITS_PER_ONE) {
            "Guard roll is out of range: $rollUnits"
        }
        if (damage <= GameNumber.ZERO || rollUnits >= guardChance(defense).units) {
            return GuardResult(damage = damage, guarded = false)
        }
        val blocked = GameMath.applyRatio(damage, Ratio.ofUnits(GUARD_BLOCK_RATIO_UNITS))
        return GuardResult(damage = damage - blocked, guarded = true)
    }
}
