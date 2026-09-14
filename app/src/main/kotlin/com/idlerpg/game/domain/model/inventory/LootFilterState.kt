package com.idlerpg.game.domain.model.inventory

import com.idlerpg.game.domain.definition.Rarity

/** Run-local deterministic loot policy used by active and offline reward resolution. */
data class LootFilterState(
    val autoSalvageEnabled: Boolean = true,
    val minimumKeepRarity: Rarity = Rarity.RARE
) {
    fun shouldKeep(rarity: Rarity): Boolean =
        !autoSalvageEnabled || rarity.rank >= minimumKeepRarity.rank
}
