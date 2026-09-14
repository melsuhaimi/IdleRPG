package com.idlerpg.game.domain.event

import com.idlerpg.game.core.id.ContentId
import com.idlerpg.game.core.id.InstanceId
import com.idlerpg.game.core.number.GameNumber
import com.idlerpg.game.domain.definition.EquipmentSlot
import com.idlerpg.game.domain.definition.Rarity

/** Completed loot/inventory/equipment/capacity facts. */
sealed interface InventoryEvent : GameEvent

data class ItemDropped(
    val itemInstanceId: InstanceId,
    val itemDefinitionId: ContentId,
    val rarity: Rarity,
    val sourceDefinitionId: ContentId? = null
) : InventoryEvent

data class ItemAdded(
    val itemInstanceId: InstanceId,
    val itemDefinitionId: ContentId
) : InventoryEvent

data class ItemSentToOverflow(
    val itemInstanceId: InstanceId,
    val itemDefinitionId: ContentId
) : InventoryEvent

data class OverflowItemClaimed(
    val itemInstanceId: InstanceId,
    val itemDefinitionId: ContentId
) : InventoryEvent

data class OverflowItemSalvaged(
    val itemInstanceId: InstanceId,
    val itemDefinitionId: ContentId,
    val goldGranted: GameNumber
) : InventoryEvent

data class InventoryCapacityExpanded(
    val quantity: Long,
    val previousCapacity: Long,
    val newCapacity: Long,
    val totalGoldCost: GameNumber
) : InventoryEvent {
    init {
        require(quantity > 0L) { "InventoryCapacityExpanded.quantity must be positive" }
        require(previousCapacity > 0L) { "previousCapacity must be positive" }
        require(newCapacity > previousCapacity) { "newCapacity must exceed previousCapacity" }
    }
}

data class InventoryProgressionBlocked(
    val normalItemCount: Long,
    val normalCapacity: Long,
    val overflowItemCount: Long,
    val overflowCapacity: Long
) : InventoryEvent

data class InventoryProgressionUnblocked(
    val availableStorageSlots: Long
) : InventoryEvent {
    init {
        require(availableStorageSlots > 0L) {
            "InventoryProgressionUnblocked requires available storage"
        }
    }
}

data class ItemEquipped(
    val itemInstanceId: InstanceId,
    val slot: EquipmentSlot
) : InventoryEvent

data class ItemUnequipped(
    val itemInstanceId: InstanceId,
    val slot: EquipmentSlot
) : InventoryEvent

data class ItemSalvaged(
    val itemInstanceId: InstanceId,
    val itemDefinitionId: ContentId,
    val goldGranted: GameNumber
) : InventoryEvent

data class ItemAutoSalvaged(
    val itemInstanceId: InstanceId,
    val itemDefinitionId: ContentId,
    val rarity: Rarity,
    val goldGranted: GameNumber
) : InventoryEvent

data class LootFilterConfigured(
    val autoSalvageEnabled: Boolean,
    val minimumKeepRarity: Rarity
) : InventoryEvent

data class ItemLocked(
    val itemInstanceId: InstanceId
) : InventoryEvent

data class ItemUnlocked(
    val itemInstanceId: InstanceId
) : InventoryEvent

data class AffixRolled(
    val itemInstanceId: InstanceId,
    val affixId: ContentId,
    val value: Long
) : InventoryEvent {
    init {
        require(value >= 0L) { "AffixRolled.value cannot be negative" }
    }
}
