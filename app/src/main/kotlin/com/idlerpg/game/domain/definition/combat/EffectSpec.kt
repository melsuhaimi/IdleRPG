package com.idlerpg.game.domain.definition.combat

import com.idlerpg.game.core.id.ContentId
import com.idlerpg.game.core.number.GameNumber
import com.idlerpg.game.core.number.Ratio
import com.idlerpg.game.domain.definition.DamageKind
import com.idlerpg.game.domain.definition.Affinity

/** Bounded authored combat vocabulary. No arbitrary executable content is permitted. */
sealed interface EffectSpec {

    enum class DamageScalingPolicy {
        ATTACK_POWER,
        ATTACK_AND_EFFECT_POWER
    }

    enum class TargetPattern {
        SELECTED,
        SELF,
        ALL_ENEMIES,
        ADJACENT_ENEMIES,
        OTHER_ENEMIES
    }

    /** Explicit deterministic predicates that can add power to one damage hit. */
    sealed interface DamageCondition {
        val bonusPowerRatio: Ratio

        data class TargetHasStatus(
            val statusDefinitionId: ContentId,
            override val bonusPowerRatio: Ratio
        ) : DamageCondition {
            init {
                require(bonusPowerRatio > Ratio.ZERO)
                require(bonusPowerRatio.units <= 50_000L)
            }
        }

        data class TargetHealthAtOrBelow(
            val threshold: Ratio,
            override val bonusPowerRatio: Ratio
        ) : DamageCondition {
            init {
                require(threshold > Ratio.ZERO && threshold <= Ratio.ONE)
                require(bonusPowerRatio > Ratio.ZERO)
                require(bonusPowerRatio.units <= 50_000L)
            }
        }
    }

    data class DealDamage(
        val powerRatio: Ratio = Ratio.ONE,
        val flatBonus: GameNumber = GameNumber.ZERO,
        val damageKind: DamageKind = DamageKind.PHYSICAL,
        val scalingPolicy: DamageScalingPolicy = DamageScalingPolicy.ATTACK_POWER,
        val canCritical: Boolean = true,
        val targetPattern: TargetPattern = TargetPattern.SELECTED,
        val hitCount: Int = 1,
        val conditions: List<DamageCondition> = emptyList()
    ) : EffectSpec {
        init {
            require(hitCount in 1..8)
            require(targetPattern != TargetPattern.SELF)
            require(conditions.size <= 2)
            require(powerRatio.units <= 100_000L)
            require(conditions.map { it::class }.size == conditions.map { it::class }.toSet().size)
        }
    }

    data class Heal(
        val flatAmount: GameNumber,
        val targetPattern: TargetPattern = TargetPattern.SELF
    ) : EffectSpec {
        init {
            require(flatAmount > GameNumber.ZERO)
            require(
                targetPattern != TargetPattern.ALL_ENEMIES &&
                    targetPattern != TargetPattern.ADJACENT_ENEMIES &&
                    targetPattern != TargetPattern.OTHER_ENEMIES
            )
        }
    }

    data class ApplyStatus(
        val statusDefinitionId: ContentId,
        val targetPattern: TargetPattern = TargetPattern.SELECTED
    ) : EffectSpec

    data class RemoveStatus(
        val statusDefinitionId: ContentId,
        val targetPattern: TargetPattern = TargetPattern.SELECTED
    ) : EffectSpec

    /** Transfers bounded existing charge without generating sequence symbols. */
    data class ShiftResonance(
        val fromAffinity: Affinity,
        val toAffinity: Affinity,
        val amount: GameNumber
    ) : EffectSpec {
        init {
            require(fromAffinity != toAffinity)
            require(amount > GameNumber.ZERO && amount <= GameNumber.of(10L))
        }
    }
}
