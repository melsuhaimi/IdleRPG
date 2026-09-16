package com.idlerpg.game.domain.system.inventory

import com.idlerpg.game.core.number.GameMath
import com.idlerpg.game.core.number.GameNumber
import com.idlerpg.game.core.number.Ratio
import com.idlerpg.game.domain.definition.Rarity
import com.idlerpg.game.domain.model.inventory.EnhancementLevel

/** Shared rarity-to-power curve used by combat modifiers and item detail projections. */
object EquipmentScalingSystem {
    /** Common is the authored baseline; higher rarities gain controlled additive power. */
    fun rarityMultiplier(rarity: Rarity): Ratio = when (rarity) {
        Rarity.COMMON -> Ratio.ONE
        Rarity.UNCOMMON -> Ratio.ofUnits(10_500L)
        Rarity.RARE -> Ratio.ofUnits(11_000L)
        Rarity.EPIC -> Ratio.ofUnits(11_800L)
        Rarity.LEGENDARY -> Ratio.ofUnits(13_000L)
    }

    /** Base-stat enhancement multiplier; rolled substats intentionally bypass this curve. */
    fun enhancementMultiplier(enhancementLevel: Int): Ratio {
        require(enhancementLevel in EnhancementLevel.INITIAL..EnhancementLevel.MAX) {
            "Unknown enhancement level: $enhancementLevel"
        }
        return Ratio.ofUnits(
            Ratio.UNITS_PER_ONE + enhancementLevel.toLong() * ENHANCEMENT_UNITS_PER_LEVEL
        )
    }

    fun scaleFlat(
        value: GameNumber,
        rarity: Rarity,
        enhancementLevel: Int = EnhancementLevel.INITIAL
    ): GameNumber =
        GameMath.applyRatio(
            GameMath.applyRatio(value, rarityMultiplier(rarity)),
            enhancementMultiplier(enhancementLevel)
        )

    private const val ENHANCEMENT_UNITS_PER_LEVEL: Long = 300L
}
