package com.idlerpg.game.domain.definition.item

import com.idlerpg.game.core.id.ContentId
import com.idlerpg.game.domain.definition.Rarity
import com.idlerpg.game.core.number.Ratio

/** One weighted item candidate inside a loot table. */
data class LootTableEntry(
    val itemDefinitionId: ContentId,
    val weight: Long,
    val rarityWeights: Map<Rarity, Long>
) {
    init {
        require(weight > 0L) {
            "LootTableEntry.weight must be positive for $itemDefinitionId"
        }
        require(rarityWeights.isNotEmpty()) {
            "LootTableEntry.rarityWeights cannot be empty for $itemDefinitionId"
        }
        require(rarityWeights.values.all { it >= 0L }) {
            "LootTableEntry rarity weights cannot be negative for $itemDefinitionId"
        }
        require(rarityWeights.values.any { it > 0L }) {
            "LootTableEntry requires at least one positive rarity weight for $itemDefinitionId"
        }
    }
}

/**
 * Weighted authored loot source.
 *
 * Foundation 11 performs [rolls] independent item selections. A roll always selects one
 * entry; future optional/no-drop behavior can be represented by encounter reward rules or
 * an explicit later loot primitive rather than an implicit null result.
 */
data class LootTableDefinition(
    val id: ContentId,
    val rolls: Int = 1,
    val dropChancePerRoll: Ratio = Ratio.ONE,
    val entries: List<LootTableEntry>
) {
    init {
        require(rolls > 0) {
            "LootTableDefinition.rolls must be positive for $id"
        }
        require(dropChancePerRoll >= Ratio.ZERO && dropChancePerRoll <= Ratio.ONE) {
            "LootTableDefinition.dropChancePerRoll must be between 0 and 1 for $id"
        }
        require(entries.isNotEmpty()) {
            "LootTableDefinition.entries cannot be empty for $id"
        }
        require(entries.map { it.itemDefinitionId }.size ==
            entries.map { it.itemDefinitionId }.toSet().size
        ) {
            "LootTableDefinition cannot repeat itemDefinitionId values for $id"
        }
    }
}
