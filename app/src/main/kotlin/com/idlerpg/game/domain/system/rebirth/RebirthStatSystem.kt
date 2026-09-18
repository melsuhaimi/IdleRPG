package com.idlerpg.game.domain.system.rebirth

import com.idlerpg.game.core.number.GameMath
import com.idlerpg.game.core.number.GameNumber
import com.idlerpg.game.core.number.Ratio
import com.idlerpg.game.domain.model.player.BaseStats
import com.idlerpg.game.domain.model.rebirth.RebirthState
import com.idlerpg.game.domain.model.rebirth.RebirthPointPool
import com.idlerpg.game.domain.model.rebirth.RebirthStat

/** Converts permanent Rebirth and Legacy points into base-stat contributions. */
object RebirthStatSystem {
    const val ATTACK_POWER_PER_POINT: Long = 1L
    const val MAX_HEALTH_PER_POINT: Long = 10L
    const val ARMOR_PER_POINT: Long = 1L
    const val ACTION_SPEED_UNITS_PER_POINT: Long = 25L
    const val CRITICAL_CHANCE_UNITS_PER_POINT: Long = 10L
    const val CRITICAL_MULTIPLIER_UNITS_PER_POINT: Long = 25L
    const val EFFECT_POWER_UNITS_PER_POINT: Long = 10L
    const val HEALING_POWER_UNITS_PER_POINT: Long = 10L
    const val LEGENDARY_FIND_WEIGHT_PER_POINT: Long = 1L
    const val MAX_LEGENDARY_FIND_WEIGHT: Long = 500L

    /**
     * Returns a small additive rarity-weight bonus. It never multiplies the table, changes
     * drop rolls, or becomes a guarantee; the cap keeps long-lived Legacy investment bounded.
     */
    fun legendaryLootBonusWeight(state: RebirthState): Long {
        val legacy = state.allocation(RebirthPointPool.LEGACY, RebirthStat.LEGENDARY_FIND)
        return minOf(
            MAX_LEGENDARY_FIND_WEIGHT,
            Math.multiplyExact(
                legacy,
                LEGENDARY_FIND_WEIGHT_PER_POINT
            )
        )
    }

    fun apply(base: BaseStats, state: RebirthState): BaseStats =
        applyPool(base, state.normalAllocations)

    private fun applyPool(
        base: BaseStats,
        allocations: Map<RebirthStat, Long>
    ): BaseStats {
        var result = base
        result = result.copy(
            attackPower = result.attackPower + pointsAsGameNumber(
                allocations[RebirthStat.ATTACK_POWER],
                ATTACK_POWER_PER_POINT
            ),
            maxHealth = result.maxHealth + pointsAsGameNumber(
                allocations[RebirthStat.MAX_HEALTH],
                MAX_HEALTH_PER_POINT
            ),
            armor = result.armor + pointsAsGameNumber(
                allocations[RebirthStat.ARMOR],
                ARMOR_PER_POINT
            ),
            actionSpeed = GameMath.ratioAfterSteps(
                result.actionSpeed,
                Ratio.ofUnits(ACTION_SPEED_UNITS_PER_POINT),
                allocations[RebirthStat.ACTION_SPEED] ?: 0L
            ),
            criticalChance = GameMath.ratioAfterSteps(
                result.criticalChance,
                Ratio.ofUnits(CRITICAL_CHANCE_UNITS_PER_POINT),
                allocations[RebirthStat.CRITICAL_CHANCE] ?: 0L
            ),
            criticalMultiplier = GameMath.ratioAfterSteps(
                result.criticalMultiplier,
                Ratio.ofUnits(CRITICAL_MULTIPLIER_UNITS_PER_POINT),
                allocations[RebirthStat.CRITICAL_MULTIPLIER] ?: 0L
            ),
            effectPower = GameMath.ratioAfterSteps(
                result.effectPower,
                Ratio.ofUnits(EFFECT_POWER_UNITS_PER_POINT),
                allocations[RebirthStat.EFFECT_POWER] ?: 0L
            ),
            healingPower = GameMath.ratioAfterSteps(
                result.healingPower,
                Ratio.ofUnits(HEALING_POWER_UNITS_PER_POINT),
                allocations[RebirthStat.HEALING_POWER] ?: 0L
            )
        )
        return result
    }

    private fun pointsAsGameNumber(points: Long?, valuePerPoint: Long): GameNumber =
        points?.let { GameNumber.of(valuePerPoint) * it } ?: GameNumber.ZERO
}
