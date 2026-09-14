package com.idlerpg.game.domain.definition.economy

import com.idlerpg.game.core.id.ContentId
import com.idlerpg.game.core.number.GameNumber
import com.idlerpg.game.core.number.Ratio
import com.idlerpg.game.domain.definition.CurrencyId

/** Bounded vocabulary of upgrade effects interpreted by derived-stat systems. */
sealed interface UpgradeEffectDefinition {

    /** Adds [amountPerLevel] to derived attack power for every purchased level. */
    data class FlatAttackPowerPerLevel(
        val amountPerLevel: GameNumber
    ) : UpgradeEffectDefinition {
        init {
            require(amountPerLevel > GameNumber.ZERO) {
                "FlatAttackPowerPerLevel.amountPerLevel must be greater than zero"
            }
        }
    }

    data class FlatMaxHealthPerLevel(val amountPerLevel: GameNumber) : UpgradeEffectDefinition {
        init { require(amountPerLevel > GameNumber.ZERO) }
    }
    data class FlatArmorPerLevel(val amountPerLevel: GameNumber) : UpgradeEffectDefinition {
        init { require(amountPerLevel > GameNumber.ZERO) }
    }
    data class ActionSpeedPerLevel(val ratioPerLevel: Ratio) : UpgradeEffectDefinition {
        init { require(ratioPerLevel > Ratio.ZERO) }
    }
    data class CriticalChancePerLevel(val ratioPerLevel: Ratio) : UpgradeEffectDefinition {
        init { require(ratioPerLevel > Ratio.ZERO) }
    }
    data class CriticalMultiplierPerLevel(val ratioPerLevel: Ratio) : UpgradeEffectDefinition {
        init { require(ratioPerLevel > Ratio.ZERO) }
    }
    data class EffectPowerPerLevel(val ratioPerLevel: Ratio) : UpgradeEffectDefinition {
        init { require(ratioPerLevel > Ratio.ZERO) }
    }
    data class HealingPowerPerLevel(val ratioPerLevel: Ratio) : UpgradeEffectDefinition {
        init { require(ratioPerLevel > Ratio.ZERO) }
    }
}

/** A milestone grants additional equivalent levels of the track's existing bounded effect. */
data class UpgradeMilestoneDefinition(
    val level: Long,
    val bonusEquivalentLevels: Long
) {
    init {
        require(level > 0L)
        require(bonusEquivalentLevels > 0L)
    }
}

/**
 * Static authored run-upgrade content.
 *
 * Purchase state stores only level by stable [id]. Current cost and final attack power
 * remain derived from this definition plus canonical state.
 */
data class UpgradeDefinition(
    val id: ContentId,
    val currencyId: CurrencyId,
    val costFormulaId: ContentId,
    val effect: UpgradeEffectDefinition,
    val milestones: List<UpgradeMilestoneDefinition> = emptyList(),
    val maxLevel: Long? = null
) {
    private val milestoneBonusTotal: Long = milestones.fold(0L) { total, milestone ->
        Math.addExact(total, milestone.bonusEquivalentLevels)
    }

    /** Largest stored level that can always expand through every authored milestone. */
    val maximumPurchasedLevel: Long = minOf(
        maxLevel ?: Long.MAX_VALUE,
        Long.MAX_VALUE - milestoneBonusTotal
    )

    init {
        require(maxLevel == null || maxLevel > 0L) {
            "UpgradeDefinition.maxLevel must be positive when present for $id"
        }
        require(milestones.map { it.level }.size == milestones.map { it.level }.toSet().size) {
            "UpgradeDefinition milestones must be unique for $id"
        }
        require(milestones.zipWithNext().all { (left, right) -> left.level < right.level }) {
            "UpgradeDefinition milestones must be strictly ordered for $id"
        }
        require(maxLevel == null || milestones.all { it.level <= maxLevel }) {
            "UpgradeDefinition milestones must not exceed maxLevel for $id"
        }
    }


    fun effectiveLevel(purchasedLevel: Long): Long = milestones
        .also {
            require(purchasedLevel in 0L..maximumPurchasedLevel) {
                "Purchased level $purchasedLevel exceeds the safe limit for $id"
            }
        }
        .takeWhile { it.level <= purchasedLevel }
        .fold(purchasedLevel) { total, milestone ->
            Math.addExact(total, milestone.bonusEquivalentLevels)
        }
}
