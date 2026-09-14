package com.idlerpg.game.domain.definition

import com.idlerpg.game.core.id.ContentId

/**
 * Stable ordered item-rarity vocabulary.
 *
 * Mechanical stat scaling is deliberately deferred to Foundation 12. Foundation 11 uses
 * rarity only for loot generation, affix count, salvage data, and ownership metadata.
 */
enum class Rarity(
    val id: ContentId,
    val rank: Int
) {
    COMMON(ContentId("rarity.common"), 0),
    UNCOMMON(ContentId("rarity.uncommon"), 1),
    RARE(ContentId("rarity.rare"), 2),
    EPIC(ContentId("rarity.epic"), 3),
    LEGENDARY(ContentId("rarity.legendary"), 4);

    companion object {
        fun ordered(): List<Rarity> = values().sortedBy { it.rank }
    }
}
