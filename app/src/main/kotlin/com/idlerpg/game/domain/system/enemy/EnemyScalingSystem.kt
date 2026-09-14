package com.idlerpg.game.domain.system.enemy

import com.idlerpg.game.core.number.GameNumber
import com.idlerpg.game.core.number.GameMath
import com.idlerpg.game.core.number.Ratio
import com.idlerpg.game.core.time.GameDuration
import com.idlerpg.game.domain.definition.enemy.EnemyDefinition
import com.idlerpg.game.domain.definition.enemy.EnemyAttackDefinition
import com.idlerpg.game.domain.definition.world.RegionDefinition
import com.idlerpg.game.domain.system.combat.CombatMath

/**
 * Deterministic enemy scaling derived from world progression.
 *
 * Encounter index 1 maps to scaling tier 0. Each later cleared tier increments the
 * scaling tier by one. Role profiles scale health, damage, armor, cadence, and rewards
 * independently, so a tank is not just a larger copy of a swarm enemy.
 */
object EnemyScalingSystem {

    fun scalingTierForEncounter(encounterIndex: Long): Long {
        require(encounterIndex > 0L) {
            "encounterIndex must be positive: $encounterIndex"
        }
        return encounterIndex - 1L
    }

    fun scaledHealth(
        enemyDefinition: EnemyDefinition,
        regionDefinition: RegionDefinition?,
        scalingTier: Long
    ): GameNumber {
        require(scalingTier >= 0L) {
            "scalingTier cannot be negative: $scalingTier"
        }

        val profile = enemyDefinition.scalingProfile
            ?: com.idlerpg.game.domain.definition.enemy.EnemyScalingProfile.forRole(enemyDefinition.role)
        val roleScaled = GameMath.scaleByStep(
            value = enemyDefinition.baseHealth,
            growthPerStep = profile.healthGrowthPerTier,
            steps = scalingTier
        )
        val regionalGrowth = regionDefinition?.enemyHealthGrowthPerTier ?: GameNumber.ZERO
        return roleScaled + (regionalGrowth * scalingTier)
    }

    fun scaledArmor(
        enemyDefinition: EnemyDefinition,
        scalingTier: Long
    ): GameNumber {
        require(scalingTier >= 0L) { "scalingTier cannot be negative: $scalingTier" }
        val profile = enemyDefinition.scalingProfile
            ?: com.idlerpg.game.domain.definition.enemy.EnemyScalingProfile.forRole(enemyDefinition.role)
        return enemyDefinition.baseArmor + (profile.armorGrowthPerTier * scalingTier)
    }

    fun scaledAttack(
        enemyDefinition: EnemyDefinition,
        attackDefinition: EnemyAttackDefinition,
        scalingTier: Long
    ): GameNumber {
        require(scalingTier >= 0L) { "scalingTier cannot be negative: $scalingTier" }
        val profile = enemyDefinition.scalingProfile
            ?: com.idlerpg.game.domain.definition.enemy.EnemyScalingProfile.forRole(enemyDefinition.role)
        return GameMath.scaleByStep(
            value = attackDefinition.baseDamage,
            growthPerStep = profile.attackGrowthPerTier,
            steps = scalingTier
        )
    }

    fun scaledAttackInterval(
        enemyDefinition: EnemyDefinition,
        attackDefinition: EnemyAttackDefinition,
        scalingTier: Long
    ): GameDuration {
        require(scalingTier >= 0L) { "scalingTier cannot be negative: $scalingTier" }
        val profile = enemyDefinition.scalingProfile
            ?: com.idlerpg.game.domain.definition.enemy.EnemyScalingProfile.forRole(enemyDefinition.role)
        val actionSpeed = GameMath.ratioAfterSteps(
            base = Ratio.ONE,
            growthPerStep = profile.actionSpeedGrowthPerTier,
            steps = scalingTier
        )
        return CombatMath.actionInterval(attackDefinition.interval, actionSpeed)
    }

    fun scaledExperienceReward(
        enemyDefinition: EnemyDefinition,
        scalingTier: Long
    ): GameNumber {
        val profile = enemyDefinition.scalingProfile
            ?: com.idlerpg.game.domain.definition.enemy.EnemyScalingProfile.forRole(enemyDefinition.role)
        return GameMath.scaleByStep(
            value = enemyDefinition.baseExperienceReward,
            growthPerStep = profile.experienceGrowthPerTier,
            steps = scalingTier
        )
    }

    fun scaledGoldReward(
        enemyDefinition: EnemyDefinition,
        scalingTier: Long
    ): GameNumber {
        val profile = enemyDefinition.scalingProfile
            ?: com.idlerpg.game.domain.definition.enemy.EnemyScalingProfile.forRole(enemyDefinition.role)
        return GameMath.scaleByStep(
            value = enemyDefinition.baseGoldReward,
            growthPerStep = profile.goldGrowthPerTier,
            steps = scalingTier
        )
    }
}
