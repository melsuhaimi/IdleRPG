package com.idlerpg.game.domain.system.stats

import com.idlerpg.game.core.number.GameNumber
import com.idlerpg.game.data.content.ContentRegistry
import com.idlerpg.game.domain.model.GameState
import com.idlerpg.game.domain.system.combat.CombatMath
import com.idlerpg.game.domain.system.skill.SkillScalingSystem

/**
 * Explanatory build-strength readout.
 *
 * Power Score is presentation data, never an input to combat, loot, progression, or RNG.
 * Every component is independently returned so the UI can explain exactly where the total
 * came from.
 */
data class PowerScoreBreakdown(
    val offense: GameNumber,
    val defense: GameNumber,
    val gear: GameNumber,
    val skills: GameNumber,
    val rebirth: GameNumber,
    val total: GameNumber,
    val expectedBasicAttackDamage: GameNumber,
    val effectiveHealth: GameNumber
) {
    init {
        require(offense >= GameNumber.ZERO)
        require(defense >= GameNumber.ZERO)
        require(gear >= GameNumber.ZERO)
        require(skills >= GameNumber.ZERO)
        require(rebirth >= GameNumber.ZERO)
        require(total == offense + defense + gear + skills + rebirth) {
            "Power Score total must equal its visible components"
        }
    }
}

object PowerScoreSystem {
    fun calculate(
        state: GameState,
        contentRegistry: ContentRegistry
    ): PowerScoreBreakdown {
        val attack = DerivedStatSystem.attackPower(state, contentRegistry)
        val critChance = DerivedStatSystem.criticalChance(state, contentRegistry)
        val critMultiplier = DerivedStatSystem.criticalMultiplier(state, contentRegistry)
        val expectedDamage = CombatMath.expectedCriticalDamage(
            baseDamage = attack,
            criticalChance = critChance,
            criticalMultiplier = critMultiplier
        )
        val maximumHealth = DerivedStatSystem.maximumHealth(state, contentRegistry)
        val armor = DerivedStatSystem.armor(state, contentRegistry)
        val effectiveHealth = maximumHealth + (armor * 10L)
        val offense = expectedDamage
        val defense = effectiveHealth
        val gear = equippedGearScore(state)
        val skills = skillScore(state, contentRegistry)
        val rebirth = rebirthScore(state)

        return PowerScoreBreakdown(
            offense = offense,
            defense = defense,
            gear = gear,
            skills = skills,
            rebirth = rebirth,
            total = offense + defense + gear + skills + rebirth,
            expectedBasicAttackDamage = expectedDamage,
            effectiveHealth = effectiveHealth
        )
    }

    private fun equippedGearScore(state: GameState): GameNumber =
        state.run.inventory.equipment.itemBySlot.values
            .distinct()
            .mapNotNull { state.run.inventory.itemsById[it] }
            .fold(GameNumber.ZERO) { total, item ->
                val rarityScore = GameNumber.of(item.rarity.rank.toLong() * RARITY_SCORE)
                val enhancementScore =
                    GameNumber.of(item.enhancementLevel.toLong() * ENHANCEMENT_SCORE)
                val rollScore = item.affixes
                    .asSequence()
                    .plus(item.mainStat?.let { sequenceOf(it) } ?: emptySequence())
                    .map { GameNumber.of(it.value) }
                    .fold(GameNumber.ZERO, GameNumber::plus)
                total + rarityScore + enhancementScore + rollScore
            }

    private fun skillScore(
        state: GameState,
        contentRegistry: ContentRegistry
    ): GameNumber =
        contentRegistry.allSkills()
            .filter { definition ->
                !definition.requiresEquipped ||
                    definition.id in state.run.player.equippedSkillIds
            }
            .fold(GameNumber.ZERO) { total, definition ->
                val rank = SkillScalingSystem.rank(state, definition)
                val mastery = SkillScalingSystem.mastery(state, definition)
                val refinement = SkillScalingSystem.refinement(state, definition)
                total +
                    GameNumber.of(rank * RANK_SCORE) +
                    GameNumber.of(mastery * MASTERY_SCORE) +
                    GameNumber.of(refinement * REFINEMENT_SCORE)
            }

    private fun rebirthScore(state: GameState): GameNumber {
        val normal = state.meta.rebirth.normalAllocations.values.sumOf { it }
        val legacy = state.meta.rebirth.legacyAllocations.values.sumOf { it }
        return GameNumber.of((normal + legacy) * REBIRTH_POINT_SCORE)
    }

    private const val RARITY_SCORE: Long = 1_000L
    private const val ENHANCEMENT_SCORE: Long = 250L
    private const val RANK_SCORE: Long = 100L
    private const val MASTERY_SCORE: Long = 25L
    private const val REFINEMENT_SCORE: Long = 100L
    private const val REBIRTH_POINT_SCORE: Long = 100L
}
