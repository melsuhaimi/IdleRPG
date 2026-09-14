package com.idlerpg.game.domain.system.loot

import com.idlerpg.game.core.random.GameRandom
import com.idlerpg.game.core.random.WeightedValue
import com.idlerpg.game.data.content.ContentRegistry
import com.idlerpg.game.domain.definition.Rarity
import com.idlerpg.game.domain.definition.item.AffixDefinition
import com.idlerpg.game.domain.definition.item.ItemDefinition
import com.idlerpg.game.domain.model.inventory.RolledAffix

/** Deterministic affix generation for one item instance. */
object AffixRollSystem {

    fun roll(
        itemDefinition: ItemDefinition,
        rarity: Rarity,
        contentRegistry: ContentRegistry,
        random: GameRandom
    ): List<RolledAffix> {
        val requestedCount = affixCountFor(rarity)
        if (requestedCount == 0 || itemDefinition.allowedAffixIds.isEmpty()) {
            return emptyList()
        }

        val equipmentDefinition =
            itemDefinition.equipmentDefinitionId
                ?.let(contentRegistry::equipment)

        val candidates =
            itemDefinition.allowedAffixIds
                .map(contentRegistry::affix)
                .filter { affix ->
                    equipmentDefinition == null ||
                        equipmentDefinition.slot in affix.compatibleSlots
                }
                .sortedBy { it.id }
                .toMutableList()

        val count = minOf(requestedCount, candidates.size)
        val result = mutableListOf<RolledAffix>()

        repeat(count) {
            val selected = chooseAffix(
                candidates = candidates,
                random = random
            )
            candidates.remove(selected)

            result += RolledAffix(
                affixId = selected.id,
                value = rollValue(
                    definition = selected,
                    random = random
                )
            )
        }

        return result.sortedBy { it.affixId }
    }

    fun affixCountFor(rarity: Rarity): Int =
        when (rarity) {
            Rarity.COMMON -> 0
            Rarity.UNCOMMON -> 1
            Rarity.RARE -> 1
            Rarity.EPIC -> 2
            Rarity.LEGENDARY -> 3
        }

    private fun chooseAffix(
        candidates: List<AffixDefinition>,
        random: GameRandom
    ): AffixDefinition =
        random.chooseWeighted(
            candidates
                .sortedBy { it.id }
                .map { definition ->
                    WeightedValue(
                        value = definition,
                        weight = definition.selectionWeight
                    )
                }
        )

    private fun rollValue(
        definition: AffixDefinition,
        random: GameRandom
    ): Long {
        val rangeSize = Math.addExact(
            Math.subtractExact(
                definition.maximumRollValue,
                definition.minimumRollValue
            ),
            1L
        )
        return Math.addExact(
            definition.minimumRollValue,
            random.nextLong(rangeSize)
        )
    }
}
