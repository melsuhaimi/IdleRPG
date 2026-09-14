package com.idlerpg.game.domain.definition.item

import com.idlerpg.game.core.id.ContentId
import com.idlerpg.game.core.number.GameNumber
import com.idlerpg.game.domain.definition.EquipmentSlot
import com.idlerpg.game.domain.definition.Affinity

/** Bounded gameplay meaning for a rolled affix scalar. */
sealed interface AffixEffectDefinition {
    /** final flat ATK contribution = rolled value × amountPerRollUnit */
    data class FlatAttackPowerPerRollUnit(
        val amountPerRollUnit: GameNumber = GameNumber.ONE
    ) : AffixEffectDefinition

    data class FlatArmorPerRollUnit(
        val amountPerRollUnit: GameNumber = GameNumber.ONE
    ) : AffixEffectDefinition

    data class ResonanceChargeBonus(
        val affinity: Affinity,
        val amountPerEmission: GameNumber = GameNumber.ONE
    ) : AffixEffectDefinition
}

/** Authored deterministic affix roll source plus its Foundation 12 mechanical meaning. */
data class AffixDefinition(
    val id: ContentId,
    val displayName: String,
    val compatibleSlots: Set<EquipmentSlot>,
    val selectionWeight: Long,
    val minimumRollValue: Long,
    val maximumRollValue: Long,
    val effect: AffixEffectDefinition? = null
) {
    init {
        require(displayName.isNotBlank()) {
            "AffixDefinition.displayName cannot be blank for $id"
        }
        require(compatibleSlots.isNotEmpty()) {
            "AffixDefinition.compatibleSlots cannot be empty for $id"
        }
        require(selectionWeight > 0L) {
            "AffixDefinition.selectionWeight must be positive for $id"
        }
        require(minimumRollValue >= 0L) {
            "AffixDefinition.minimumRollValue cannot be negative for $id"
        }
        require(maximumRollValue >= minimumRollValue) {
            "AffixDefinition roll range is invalid for $id"
        }
        Math.addExact(
            Math.subtractExact(maximumRollValue, minimumRollValue),
            1L
        )
        when (val authoredEffect = effect) {
            is AffixEffectDefinition.FlatAttackPowerPerRollUnit ->
                require(authoredEffect.amountPerRollUnit > GameNumber.ZERO) {
                    "Affix effect amountPerRollUnit must be > 0 for $id"
                }
            is AffixEffectDefinition.FlatArmorPerRollUnit ->
                require(authoredEffect.amountPerRollUnit > GameNumber.ZERO)
            is AffixEffectDefinition.ResonanceChargeBonus ->
                require(authoredEffect.amountPerEmission > GameNumber.ZERO)
            null -> Unit
        }
    }
}
