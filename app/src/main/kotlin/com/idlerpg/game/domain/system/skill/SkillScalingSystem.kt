package com.idlerpg.game.domain.system.skill

import com.idlerpg.game.core.number.GameMath
import com.idlerpg.game.core.number.Ratio
import com.idlerpg.game.domain.definition.combat.EffectSpec
import com.idlerpg.game.domain.definition.combat.SkillDefinition
import com.idlerpg.game.domain.model.GameState
import com.idlerpg.game.domain.system.stats.PlayerScalingSystem

/**
 * Applies the shared player-level baseline and the explicit per-skill investment tracks.
 *
 * Rank is never allowed to exceed the authored skill cap. Mastery and refinement are small
 * additive multipliers so their effect is visible but cannot replace gear, stats, or combat
 * decisions as the primary source of power.
 */
object SkillScalingSystem {
    fun rank(state: GameState, definition: SkillDefinition): Long {
        val levelRank = PlayerScalingSystem.skillRank(
            playerLevel = state.run.progression.playerLevel.level,
            maximumRank = definition.maxRank
        )
        val investedRank =
            state.run.progression.skillProgression.rankBySkillId[definition.id] ?: 1L
        val maximum = definition.maxRank ?: Long.MAX_VALUE
        return maxOf(levelRank, investedRank).coerceAtMost(maximum)
    }

    fun mastery(state: GameState, definition: SkillDefinition): Long =
        state.run.progression.skillProgression.masteryBySkillId[definition.id] ?: 0L

    fun refinement(state: GameState, definition: SkillDefinition): Long =
        state.run.progression.skillProgression.refinementBySkillId[definition.id] ?: 0L

    fun scaledEffects(
        state: GameState,
        definition: SkillDefinition
    ): List<EffectSpec> {
        val steps = rank(state, definition) - 1L
        val rankMultiplier = GameMath.ratioAfterSteps(
            base = Ratio.ONE,
            growthPerStep = definition.powerGrowthPerPlayerLevel,
            steps = steps
        )
        val masteryDamageMultiplier = GameMath.ratioAfterSteps(
            base = Ratio.ONE,
            growthPerStep = Ratio.ofUnits(150L),
            steps = mastery(state, definition)
        )
        val refinementDamageMultiplier = GameMath.ratioAfterSteps(
            base = Ratio.ONE,
            growthPerStep = Ratio.ofUnits(500L),
            steps = refinement(state, definition)
        )
        val damageMultiplier = GameMath.multiplyRatios(
            rankMultiplier,
            GameMath.multiplyRatios(masteryDamageMultiplier, refinementDamageMultiplier)
        )
        val masteryHealingMultiplier = GameMath.ratioAfterSteps(
            base = Ratio.ONE,
            growthPerStep = Ratio.ofUnits(100L),
            steps = mastery(state, definition)
        )
        val refinementHealingMultiplier = GameMath.ratioAfterSteps(
            base = Ratio.ONE,
            growthPerStep = Ratio.ofUnits(300L),
            steps = refinement(state, definition)
        )
        val healingMultiplier = GameMath.multiplyRatios(
            rankMultiplier,
            GameMath.multiplyRatios(masteryHealingMultiplier, refinementHealingMultiplier)
        )
        return definition.effects.map { effect ->
            when (effect) {
                is EffectSpec.DealDamage -> effect.copy(
                    powerRatio = GameMath.multiplyRatios(effect.powerRatio, damageMultiplier)
                )
                is EffectSpec.Heal -> {
                    val rankedAmount = GameMath.scaleByStep(
                        effect.flatAmount,
                        definition.healingGrowthPerPlayerLevel,
                        steps
                    )
                    effect.copy(flatAmount = GameMath.applyRatio(rankedAmount, healingMultiplier))
                }
                else -> effect
            }
        }
    }
}
