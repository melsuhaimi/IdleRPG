package com.idlerpg.game.domain.system.loot

import com.idlerpg.game.core.random.GameRandom
import com.idlerpg.game.core.random.WeightedValue
import com.idlerpg.game.core.id.ContentId
import com.idlerpg.game.data.content.ContentRegistry
import com.idlerpg.game.domain.definition.Rarity
import com.idlerpg.game.domain.definition.item.AffixDefinition
import com.idlerpg.game.domain.definition.item.ItemDefinition
import com.idlerpg.game.domain.model.inventory.RolledAffix

/** Deterministic affix generation for one item instance. */
object AffixRollSystem {

    fun rollMainStat(
        itemDefinition: ItemDefinition,
        contentRegistry: ContentRegistry,
        random: GameRandom,
        rarity: Rarity = Rarity.COMMON
    ): RolledAffix? {
        val candidates = candidatesFor(itemDefinition, contentRegistry)
        if (candidates.isEmpty()) return null
        val selected = chooseAffix(candidates, random)
        return RolledAffix(
            affixId = selected.id,
            value = rollValue(selected, random, rarity)
        )
    }

    fun roll(
        itemDefinition: ItemDefinition,
        rarity: Rarity,
        contentRegistry: ContentRegistry,
        random: GameRandom,
        excludedAffixIds: Set<ContentId> = emptySet()
    ): List<RolledAffix> {
        val requestedCount = affixCountFor(rarity)
        val eligible = candidatesFor(itemDefinition, contentRegistry)
        val excludedEffectKeys = eligible
            .filter { it.id in excludedAffixIds }
            .map(::mechanicalEffectKey)
            .toSet()
        val candidates = eligible
            .filterNot { it.id in excludedAffixIds }
            .filterNot { mechanicalEffectKey(it) in excludedEffectKeys }
            .toMutableList()
        if (requestedCount == 0) return emptyList()
        val availableMechanics = candidates.map(::mechanicalEffectKey).toSet().size
        require(availableMechanics >= requestedCount) {
            "${itemDefinition.id} has $availableMechanics distinct eligible affixes after exclusions; " +
                "rarity $rarity requires $requestedCount distinct substats"
        }

        val result = mutableListOf<RolledAffix>()
        repeat(requestedCount) {
            val selected = chooseAffix(candidates, random)
            val selectedEffectKey = mechanicalEffectKey(selected)
            candidates.removeAll { mechanicalEffectKey(it) == selectedEffectKey }
            result += RolledAffix(
                affixId = selected.id,
                value = rollValue(selected, random, rarity)
            )
        }
        return result.sortedBy { it.affixId }
    }

    fun affixCountFor(rarity: Rarity): Int =
        when (rarity) {
            Rarity.COMMON,
            Rarity.UNCOMMON,
            Rarity.RARE -> 3
            Rarity.EPIC,
            Rarity.LEGENDARY -> 4
        }

    private fun candidatesFor(
        itemDefinition: ItemDefinition,
        contentRegistry: ContentRegistry
    ): List<AffixDefinition> {
        val equipmentDefinition = itemDefinition.equipmentDefinitionId
            ?.let(contentRegistry::equipment)
        return itemDefinition.allowedAffixIds
            .map(contentRegistry::affix)
            .filter { affix ->
                equipmentDefinition == null ||
                    equipmentDefinition.slot in affix.compatibleSlots
            }
            .sortedBy { it.id }
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

    /** Different affix IDs may represent the same actual stat and cannot both be rolled. */
    private fun mechanicalEffectKey(definition: AffixDefinition): Any =
        definition.effect ?: definition.id

    private fun rollValue(
        definition: AffixDefinition,
        random: GameRandom,
        rarity: Rarity
    ): Long {
        val minimum = GearRollQuality.minimum(definition, rarity)
        val rangeSize = Math.addExact(
            Math.subtractExact(
                definition.maximumRollValue,
                minimum
            ),
            1L
        )
        return Math.addExact(
            minimum,
            random.nextLong(rangeSize)
        )
    }
}
