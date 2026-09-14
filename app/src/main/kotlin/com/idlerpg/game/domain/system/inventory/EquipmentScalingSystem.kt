package com.idlerpg.game.domain.system.inventory

import com.idlerpg.game.core.number.GameMath
import com.idlerpg.game.core.number.GameNumber
import com.idlerpg.game.core.number.Ratio
import com.idlerpg.game.domain.definition.Rarity

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

    fun scaleFlat(value: GameNumber, rarity: Rarity): GameNumber =
        GameMath.applyRatio(value, rarityMultiplier(rarity))
}
