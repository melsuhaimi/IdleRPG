package com.idlerpg.game.domain.definition.enemy

import com.idlerpg.game.core.number.GameNumber
import com.idlerpg.game.core.number.Ratio

/**
 * Tunable role profile for deterministic long-run enemy growth.
 *
 * Values are per encounter tier. They are deliberately additive to a 1.0 multiplier
 * instead of repeatedly multiplying the previous result, so stage 100 remains stable,
 * inspectable, and reproducible across active/offline simulation.
 */
data class EnemyScalingProfile(
    val healthGrowthPerTier: Ratio,
    val attackGrowthPerTier: Ratio,
    val armorGrowthPerTier: GameNumber,
    val actionSpeedGrowthPerTier: Ratio,
    val experienceGrowthPerTier: Ratio,
    val goldGrowthPerTier: Ratio
) {
    init {
        require(healthGrowthPerTier >= Ratio.ZERO)
        require(attackGrowthPerTier >= Ratio.ZERO)
        require(armorGrowthPerTier >= GameNumber.ZERO)
        require(actionSpeedGrowthPerTier >= Ratio.ZERO)
        require(experienceGrowthPerTier >= Ratio.ZERO)
        require(goldGrowthPerTier >= Ratio.ZERO)
    }

    companion object {
        /** Role defaults keep authored enemy definitions concise while preserving identity. */
        fun forRole(role: EnemyRole): EnemyScalingProfile = when (role) {
            EnemyRole.SWARM -> EnemyScalingProfile(
                healthGrowthPerTier = Ratio.ofUnits(600L),
                attackGrowthPerTier = Ratio.ofUnits(500L),
                armorGrowthPerTier = GameNumber.ONE,
                actionSpeedGrowthPerTier = Ratio.ZERO,
                experienceGrowthPerTier = Ratio.ofUnits(450L),
                goldGrowthPerTier = Ratio.ofUnits(400L)
            )
            EnemyRole.ASSASSIN -> EnemyScalingProfile(
                healthGrowthPerTier = Ratio.ofUnits(520L),
                attackGrowthPerTier = Ratio.ofUnits(850L),
                armorGrowthPerTier = GameNumber.ONE,
                actionSpeedGrowthPerTier = Ratio.ofUnits(120L),
                experienceGrowthPerTier = Ratio.ofUnits(550L),
                goldGrowthPerTier = Ratio.ofUnits(600L)
            )
            EnemyRole.CASTER -> EnemyScalingProfile(
                healthGrowthPerTier = Ratio.ofUnits(650L),
                attackGrowthPerTier = Ratio.ofUnits(700L),
                armorGrowthPerTier = GameNumber.ONE,
                actionSpeedGrowthPerTier = Ratio.ofUnits(40L),
                experienceGrowthPerTier = Ratio.ofUnits(650L),
                goldGrowthPerTier = Ratio.ofUnits(650L)
            )
            EnemyRole.PROTECTOR -> EnemyScalingProfile(
                healthGrowthPerTier = Ratio.ofUnits(1_100L),
                attackGrowthPerTier = Ratio.ofUnits(450L),
                armorGrowthPerTier = GameNumber.of(5L),
                actionSpeedGrowthPerTier = Ratio.ZERO,
                experienceGrowthPerTier = Ratio.ofUnits(700L),
                goldGrowthPerTier = Ratio.ofUnits(700L)
            )
            EnemyRole.DISRUPTOR,
            EnemyRole.CONTROLLER,
            EnemyRole.PARASITE,
            EnemyRole.ADAPTIVE -> EnemyScalingProfile(
                healthGrowthPerTier = Ratio.ofUnits(700L),
                attackGrowthPerTier = Ratio.ofUnits(650L),
                armorGrowthPerTier = GameNumber.of(2L),
                actionSpeedGrowthPerTier = Ratio.ofUnits(40L),
                experienceGrowthPerTier = Ratio.ofUnits(700L),
                goldGrowthPerTier = Ratio.ofUnits(700L)
            )
            EnemyRole.BOSS -> EnemyScalingProfile(
                healthGrowthPerTier = Ratio.ofUnits(1_350L),
                attackGrowthPerTier = Ratio.ofUnits(900L),
                armorGrowthPerTier = GameNumber.of(8L),
                actionSpeedGrowthPerTier = Ratio.ofUnits(40L),
                experienceGrowthPerTier = Ratio.ofUnits(1_000L),
                goldGrowthPerTier = Ratio.ofUnits(1_000L)
            )
        }
    }
}
