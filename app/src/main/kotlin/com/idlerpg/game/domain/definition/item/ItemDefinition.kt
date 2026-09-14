package com.idlerpg.game.domain.definition.item

import com.idlerpg.game.core.id.ContentId
import com.idlerpg.game.core.number.GameNumber
import com.idlerpg.game.domain.definition.Rarity

/** Broad authored item category. Foundation 11 initially authors equipment only. */
enum class ItemCategory {
    EQUIPMENT,
    MATERIAL
}

/**
 * Authored salvage output.
 *
 * Foundation 11 supports Gold-only salvage. Additional currencies/material outputs must be
 * introduced by the foundation that owns those reward types rather than hidden here.
 */
data class SalvageProfile(
    val goldByRarity: Map<Rarity, GameNumber> = emptyMap()
) {
    init {
        require(goldByRarity.values.all { it >= GameNumber.ZERO }) {
            "SalvageProfile Gold values cannot be negative"
        }
    }

    fun goldFor(rarity: Rarity): GameNumber =
        goldByRarity[rarity] ?: GameNumber.ZERO
}

/**
 * Static item archetype.
 *
 * Generated rarity, affix rolls, ownership, lock state, and equipped state belong to
 * ItemInstance/InventoryState rather than this definition.
 */
data class ItemDefinition(
    val id: ContentId,
    val displayName: String,
    val category: ItemCategory,
    val minimumRarity: Rarity = Rarity.COMMON,
    val maximumRarity: Rarity = Rarity.LEGENDARY,
    val equipmentDefinitionId: ContentId? = null,
    val allowedAffixIds: List<ContentId> = emptyList(),
    val salvageProfile: SalvageProfile = SalvageProfile()
) {
    init {
        require(displayName.isNotBlank()) {
            "ItemDefinition.displayName cannot be blank for $id"
        }
        require(minimumRarity.rank <= maximumRarity.rank) {
            "ItemDefinition rarity bounds are reversed for $id"
        }
        require(allowedAffixIds.size == allowedAffixIds.toSet().size) {
            "ItemDefinition.allowedAffixIds cannot contain duplicates for $id"
        }
        when (category) {
            ItemCategory.EQUIPMENT -> require(equipmentDefinitionId != null) {
                "Equipment item $id requires equipmentDefinitionId"
            }
            ItemCategory.MATERIAL -> require(equipmentDefinitionId == null) {
                "Material item $id cannot reference EquipmentDefinition"
            }
        }
    }

    fun allowsRarity(rarity: Rarity): Boolean =
        rarity.rank in minimumRarity.rank..maximumRarity.rank
}
