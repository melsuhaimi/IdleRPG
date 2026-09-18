package com.idlerpg.game.domain.system.loot

import com.idlerpg.game.domain.definition.Rarity
import com.idlerpg.game.domain.definition.item.AffixDefinition
import java.math.BigInteger

/** Rarity lifts the minimum quality, keeping the authored maximum and one value roll. */
object GearRollQuality {
    const val MINIMUM_PERCENT_PER_RARITY_RANK = 10L

    fun minimum(definition: AffixDefinition, rarity: Rarity): Long {
        val low = BigInteger.valueOf(definition.minimumRollValue)
        val span = BigInteger.valueOf(definition.maximumRollValue).subtract(low)
        return low.add(span.multiply(BigInteger.valueOf(rarity.rank * MINIMUM_PERCENT_PER_RARITY_RANK))
            .divide(BigInteger.valueOf(100))).longValueExact()
    }
}
