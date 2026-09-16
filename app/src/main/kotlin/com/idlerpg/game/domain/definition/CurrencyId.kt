package com.idlerpg.game.domain.definition

import com.idlerpg.game.core.id.ContentId

/**
 * Stable typed currency identifier.
 *
 * Currency identity remains backed by ContentId so save references stay stable while
 * economy APIs cannot accidentally confuse a currency ID with an enemy/skill/item ID.
 */
data class CurrencyId(
    val id: ContentId
) : Comparable<CurrencyId> {

    override fun compareTo(other: CurrencyId): Int =
        id.compareTo(other.id)

    override fun toString(): String = id.toString()

    companion object {
        val GOLD: CurrencyId = CurrencyId(
            ContentId("currency.gold")
        )
        val GEMS: CurrencyId = CurrencyId(
            ContentId("currency.gems")
        )
        val ENHANCEMENT_MATERIAL: CurrencyId = CurrencyId(
            ContentId("currency.enhancement_material")
        )
        val REFINEMENT_MATERIAL: CurrencyId = CurrencyId(
            ContentId("currency.refinement_material")
        )
    }
}
