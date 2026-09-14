package com.idlerpg.game.domain.system.stats

import com.idlerpg.game.core.number.GameMath
import com.idlerpg.game.core.number.Ratio
import com.idlerpg.game.domain.model.player.BaseStats

/**
 * Deterministic level contribution to the player's combat baseline.
 *
 * BaseStats remains the persisted level-one contract. These increments are the authored
 * level curve, applied before equipment, upgrades, and statuses. Keeping this boundary
 * separate makes balance tuning and stat explanations use the same calculation everywhere.
 */
object PlayerScalingSystem {
    const val ATTACK_PER_LEVEL: Long = 2L
    const val MAX_HEALTH_PER_LEVEL: Long = 12L
    const val ARMOR_PER_LEVEL: Long = 3L
    const val ACTION_SPEED_UNITS_PER_LEVEL: Long = 40L
    const val CRITICAL_CHANCE_UNITS_PER_LEVEL: Long = 25L
    const val CRITICAL_MULTIPLIER_UNITS_PER_LEVEL: Long = 50L
    const val EFFECT_POWER_UNITS_PER_LEVEL: Long = 30L
    const val HEALING_POWER_UNITS_PER_LEVEL: Long = 25L

    fun baseStatsForLevel(base: BaseStats, level: Long): BaseStats {
        require(level > 0L) { "Player level must be positive: $level" }
        val steps = level - 1L
        return base.copy(
            attackPower = GameMath.linearGrowth(
                base.attackPower,
                com.idlerpg.game.core.number.GameNumber.of(ATTACK_PER_LEVEL),
                steps
            ),
            maxHealth = GameMath.linearGrowth(
                base.maxHealth,
                com.idlerpg.game.core.number.GameNumber.of(MAX_HEALTH_PER_LEVEL),
                steps
            ),
            armor = GameMath.linearGrowth(
                base.armor,
                com.idlerpg.game.core.number.GameNumber.of(ARMOR_PER_LEVEL),
                steps
            ),
            actionSpeed = GameMath.ratioAfterSteps(
                base.actionSpeed,
                Ratio.ofUnits(ACTION_SPEED_UNITS_PER_LEVEL),
                steps
            ),
            criticalChance = GameMath.ratioAfterSteps(
                base.criticalChance,
                Ratio.ofUnits(CRITICAL_CHANCE_UNITS_PER_LEVEL),
                steps
            ),
            criticalMultiplier = GameMath.ratioAfterSteps(
                base.criticalMultiplier,
                Ratio.ofUnits(CRITICAL_MULTIPLIER_UNITS_PER_LEVEL),
                steps
            ),
            effectPower = GameMath.ratioAfterSteps(
                base.effectPower,
                Ratio.ofUnits(EFFECT_POWER_UNITS_PER_LEVEL),
                steps
            ),
            healingPower = GameMath.ratioAfterSteps(
                base.healingPower,
                Ratio.ofUnits(HEALING_POWER_UNITS_PER_LEVEL),
                steps
            )
        )
    }

    fun skillRank(playerLevel: Long, maximumRank: Long?): Long {
        require(playerLevel > 0L) { "Player level must be positive: $playerLevel" }
        return if (maximumRank == null) playerLevel else playerLevel.coerceAtMost(maximumRank)
    }
}
