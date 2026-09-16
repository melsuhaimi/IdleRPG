package com.idlerpg.game.domain.system.loot

import com.idlerpg.game.core.id.ContentId
import com.idlerpg.game.core.random.GameRandom
import com.idlerpg.game.core.random.WeightedValue
import com.idlerpg.game.data.content.ContentRegistry
import com.idlerpg.game.domain.definition.Rarity
import com.idlerpg.game.domain.definition.item.LootTableDefinition
import com.idlerpg.game.domain.definition.item.LootTableEntry

/** One deterministic authored loot selection before instance generation. */
data class LootSelection(
    val itemDefinitionId: ContentId,
    val rarity: Rarity
)

/** Controlled weighted loot-table selection. */
object LootTableSystem {

    fun roll(
        definition: LootTableDefinition,
        contentRegistry: ContentRegistry,
        random: GameRandom,
        legendaryBonusWeight: Long = 0L
    ): List<LootSelection> {
        require(legendaryBonusWeight in 0L..500L) {
            "legendaryBonusWeight must be bounded: " + legendaryBonusWeight
        }
        return buildList {
            repeat(definition.rolls) {
                if (random.nextLong(com.idlerpg.game.core.number.Ratio.UNITS_PER_ONE) >=
                    definition.dropChancePerRoll.units
                ) {
                    return@repeat
                }
                val entry = chooseEntry(
                    definition = definition,
                    random = random
                )
                val itemDefinition = contentRegistry.item(entry.itemDefinitionId)
                val rarity = chooseRarity(
                    entry = entry,
                    itemDefinitionAllows = itemDefinition::allowsRarity,
                    random = random,
                    legendaryBonusWeight = legendaryBonusWeight
                )
                add(
                    LootSelection(
                        itemDefinitionId = itemDefinition.id,
                        rarity = rarity
                    )
                )
            }
        }
    }

    private fun chooseEntry(
        definition: LootTableDefinition,
        random: GameRandom
    ): LootTableEntry =
        random.chooseWeighted(
            definition.entries
                .sortedBy { it.itemDefinitionId }
                .map { entry ->
                    WeightedValue(
                        value = entry,
                        weight = entry.weight
                    )
                }
        )

    private fun chooseRarity(
        entry: LootTableEntry,
        itemDefinitionAllows: (Rarity) -> Boolean,
        random: GameRandom,
        legendaryBonusWeight: Long
    ): Rarity {
        val options =
            Rarity.ordered()
                .filter(itemDefinitionAllows)
                .mapNotNull { rarity ->
                    val authoredWeight = entry.rarityWeights[rarity] ?: 0L
                    val weight = if (rarity == Rarity.LEGENDARY) {
                        Math.addExact(authoredWeight, legendaryBonusWeight)
                    } else {
                        authoredWeight
                    }
                    if (weight <= 0L) {
                        null
                    } else {
                        WeightedValue(
                            value = rarity,
                            weight = weight
                        )
                    }
                }

        require(options.isNotEmpty()) {
            "Validated loot entry ${entry.itemDefinitionId} has no eligible rarity"
        }

        return random.chooseWeighted(options)
    }
}
