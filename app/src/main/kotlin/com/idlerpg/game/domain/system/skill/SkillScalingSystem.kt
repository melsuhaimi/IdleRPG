package com.idlerpg.game.domain.system.skill

import com.idlerpg.game.core.number.GameMath
import com.idlerpg.game.core.number.Ratio
import com.idlerpg.game.domain.definition.combat.EffectSpec
import com.idlerpg.game.domain.definition.combat.SkillDefinition
import com.idlerpg.game.domain.model.GameState

/**
 * Applies the explicit per-skill investment tracks.
 *
 * Rank is independent from player level and starts at one for an unlocked skill. It is never
 * allowed to exceed the authored skill cap. Mastery and refinement are small additive
 * multipliers so their effect is visible but cannot replace gear, stats, or combat decisions as
 * the primary source of power.
 */
object SkillScalingSystem {
    const val MASTERY_DAMAGE_UNITS_PER_LEVEL: Long = 150L
    const val REFINEMENT_DAMAGE_UNITS_PER_LEVEL: Long = 500L
    const val MASTERY_HEALING_UNITS_PER_LEVEL: Long = 100L
    const val REFINEMENT_HEALING_UNITS_PER_LEVEL: Long = 300L

    fun rank(state: GameState, definition: SkillDefinition): Long =
        (state.run.progression.skillProgression.rankBySkillId[definition.id] ?: 1L)
            .coerceAtMost(definition.maxRank ?: Long.MAX_VALUE)

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
            growthPerStep = Ratio.ofUnits(MASTERY_DAMAGE_UNITS_PER_LEVEL),
            steps = mastery(state, definition)
        )
        val refinementDamageMultiplier = GameMath.ratioAfterSteps(
            base = Ratio.ONE,
            growthPerStep = Ratio.ofUnits(REFINEMENT_DAMAGE_UNITS_PER_LEVEL),
            steps = refinement(state, definition)
        )
        val damageMultiplier = GameMath.multiplyRatios(
            rankMultiplier,
            GameMath.multiplyRatios(masteryDamageMultiplier, refinementDamageMultiplier)
        )
        val masteryHealingMultiplier = GameMath.ratioAfterSteps(
            base = Ratio.ONE,
            growthPerStep = Ratio.ofUnits(MASTERY_HEALING_UNITS_PER_LEVEL),
            steps = mastery(state, definition)
        )
        val refinementHealingMultiplier = GameMath.ratioAfterSteps(
            base = Ratio.ONE,
            growthPerStep = Ratio.ofUnits(REFINEMENT_HEALING_UNITS_PER_LEVEL),
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
