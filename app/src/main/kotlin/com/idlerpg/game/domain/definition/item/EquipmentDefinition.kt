package com.idlerpg.game.domain.definition.item

import com.idlerpg.game.core.id.ContentId
import com.idlerpg.game.core.number.GameNumber
import com.idlerpg.game.domain.definition.Affinity
import com.idlerpg.game.domain.definition.EquipmentSlot
import com.idlerpg.game.domain.definition.Rarity
import com.idlerpg.game.core.number.Ratio

/** Bounded equipment effects interpreted by ModifierSystem. */
sealed interface EquipmentEffectDefinition {
    data class FlatAttackPower(
        val amount: GameNumber
    ) : EquipmentEffectDefinition

    data class FlatArmor(
        val amount: GameNumber
    ) : EquipmentEffectDefinition

    /** Adds exact charge to each matching authored Resonance emission. */
    data class ResonanceChargeBonus(
        val affinity: Affinity,
        val amountPerEmission: GameNumber
    ) : EquipmentEffectDefinition

    /** Adds a deterministic secondary strike to every other living enemy. */
    data class SkillCleave(val skillId: ContentId, val secondaryDamageRatio: Ratio) : EquipmentEffectDefinition

    /** Multiplies a skill's damage while its selected target carries the authored status. */
    data class SkillDamageAgainstStatus(
        val skillId: ContentId,
        val statusId: ContentId,
        val multiplier: Ratio
    ) : EquipmentEffectDefinition

    /** Multiplies a skill's damage against targets at or below the health threshold. */
    data class SkillExecute(
        val skillId: ContentId,
        val healthThreshold: Ratio,
        val multiplier: Ratio
    ) : EquipmentEffectDefinition

    /** Emits additional charge whenever the matching skill is used. */
    data class SkillResonanceBonus(
        val skillId: ContentId,
        val affinity: Affinity,
        val amount: GameNumber,
        val playerHealthAtMost: Ratio? = null
    ) : EquipmentEffectDefinition

    /** Scales healing produced by one skill without changing unrelated healing. */
    data class SkillHealingMultiplier(val skillId: ContentId, val multiplier: Ratio) : EquipmentEffectDefinition

    /** Restores a bounded number of pre-cast sequence entries after Convergence resolution. */
    data class SkillSequencePreservation(val skillId: ContentId, val entryCount: Int) : EquipmentEffectDefinition
}

/** Static equipment-specific behavior identity. */
data class EquipmentDefinition(
    val id: ContentId,
    val slot: EquipmentSlot,
    val effects: List<EquipmentEffectDefinition> = emptyList(),
    val traitMinimumRarity: Rarity = Rarity.RARE
) {
    init {
        effects.forEach { effect ->
            when (effect) {
                is EquipmentEffectDefinition.FlatAttackPower ->
                    require(effect.amount > GameNumber.ZERO) {
                        "FlatAttackPower amount must be > 0 for $id"
                    }

                is EquipmentEffectDefinition.FlatArmor ->
                    require(effect.amount > GameNumber.ZERO) {
                        "FlatArmor amount must be > 0 for $id"
                    }

                is EquipmentEffectDefinition.ResonanceChargeBonus ->
                    require(effect.amountPerEmission > GameNumber.ZERO) {
                        "ResonanceChargeBonus amount must be > 0 for $id"
                    }
                is EquipmentEffectDefinition.SkillCleave ->
                    require(effect.secondaryDamageRatio > Ratio.ZERO && effect.secondaryDamageRatio <= Ratio.ONE)
                is EquipmentEffectDefinition.SkillDamageAgainstStatus ->
                    require(effect.multiplier > Ratio.ZERO)
                is EquipmentEffectDefinition.SkillExecute -> {
                    require(effect.healthThreshold > Ratio.ZERO && effect.healthThreshold <= Ratio.ONE)
                    require(effect.multiplier > Ratio.ONE)
                }
                is EquipmentEffectDefinition.SkillResonanceBonus -> {
                    require(effect.amount > GameNumber.ZERO)
                    require(effect.playerHealthAtMost == null ||
                        (effect.playerHealthAtMost > Ratio.ZERO && effect.playerHealthAtMost <= Ratio.ONE))
                }
                is EquipmentEffectDefinition.SkillHealingMultiplier -> require(effect.multiplier > Ratio.ZERO)
                is EquipmentEffectDefinition.SkillSequencePreservation -> require(effect.entryCount in 1..3)
            }
        }
    }

    fun activeEffects(rarity: Rarity): List<EquipmentEffectDefinition> = effects.filter { effect ->
        !effect.isBuildDefiningTrait() || rarity.rank >= traitMinimumRarity.rank
    }

    fun hasActiveBuildDefiningTrait(rarity: Rarity): Boolean =
        rarity.rank >= traitMinimumRarity.rank && effects.any { it.isBuildDefiningTrait() }
}

fun EquipmentEffectDefinition.isBuildDefiningTrait(): Boolean =
    this !is EquipmentEffectDefinition.FlatAttackPower &&
        this !is EquipmentEffectDefinition.FlatArmor &&
        this !is EquipmentEffectDefinition.ResonanceChargeBonus
