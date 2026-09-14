package com.idlerpg.game.domain.system.skill

import com.idlerpg.game.core.number.GameMath
import com.idlerpg.game.domain.definition.combat.EffectSpec
import com.idlerpg.game.domain.definition.combat.SkillDefinition
import com.idlerpg.game.domain.model.GameState
import com.idlerpg.game.domain.system.stats.PlayerScalingSystem

/** Applies the shared player-level skill rank curve before combat modifiers. */
object SkillScalingSystem {
    fun rank(state: GameState, definition: SkillDefinition): Long =
        PlayerScalingSystem.skillRank(
            playerLevel = state.run.progression.playerLevel.level,
            maximumRank = definition.maxRank
        )

    fun scaledEffects(
        state: GameState,
        definition: SkillDefinition
    ): List<EffectSpec> {
        val steps = rank(state, definition) - 1L
        val damageMultiplier = GameMath.ratioAfterSteps(
            base = com.idlerpg.game.core.number.Ratio.ONE,
            growthPerStep = definition.powerGrowthPerPlayerLevel,
            steps = steps
        )
        return definition.effects.map { effect ->
            when (effect) {
                is EffectSpec.DealDamage -> effect.copy(
                    powerRatio = GameMath.multiplyRatios(effect.powerRatio, damageMultiplier)
                )
                is EffectSpec.Heal -> effect.copy(
                    flatAmount = GameMath.scaleByStep(
                        effect.flatAmount,
                        definition.healingGrowthPerPlayerLevel,
                        steps
                    )
                )
                else -> effect
            }
        }
    }
}
