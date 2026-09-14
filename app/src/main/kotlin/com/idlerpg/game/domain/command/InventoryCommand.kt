package com.idlerpg.game.domain.command

import com.idlerpg.game.core.id.InstanceId
import com.idlerpg.game.domain.definition.EquipmentSlot
import com.idlerpg.game.domain.definition.Rarity

/** Inventory/equipment/capacity player intent. */
sealed interface InventoryCommand : GameCommand

/** Attempt to equip an owned normal-inventory item into one typed equipment slot. */
data class EquipItem(
    val itemInstanceId: InstanceId,
    val slot: EquipmentSlot,
    override val correlationId: CommandCorrelationId? = null
) : InventoryCommand

/** Attempt to clear one equipment slot. */
data class UnequipItem(
    val slot: EquipmentSlot,
    override val correlationId: CommandCorrelationId? = null
) : InventoryCommand

/** Attempt to salvage one owned, unlocked, unequipped normal-inventory item. */
data class SalvageItem(
    val itemInstanceId: InstanceId,
    override val correlationId: CommandCorrelationId? = null
) : InventoryCommand

/** Atomically salvage an explicit selection; any invalid item rejects the whole command. */
data class SalvageItems(
    val itemInstanceIds: Set<InstanceId>,
    override val correlationId: CommandCorrelationId? = null
) : InventoryCommand {
    init { require(itemInstanceIds.isNotEmpty()) }
}

/** Atomically salvage every eligible normal-inventory item below [rarity]. */
data class SalvageAllBelow(
    val rarity: Rarity,
    override val correlationId: CommandCorrelationId? = null
) : InventoryCommand

/** Persist the deterministic active/offline loot policy. */
data class ConfigureLootFilter(
    val autoSalvageEnabled: Boolean,
    val minimumKeepRarity: Rarity,
    override val correlationId: CommandCorrelationId? = null
) : InventoryCommand

/** Attempt to protect one owned normal-inventory item from destructive actions. */
data class LockItem(
    val itemInstanceId: InstanceId,
    override val correlationId: CommandCorrelationId? = null
) : InventoryCommand

/** Attempt to remove the inventory lock from one owned normal-inventory item. */
data class UnlockItem(
    val itemInstanceId: InstanceId,
    override val correlationId: CommandCorrelationId? = null
) : InventoryCommand

/** Purchase [quantity] deterministic inventory-capacity expansions atomically. */
data class ExpandInventoryCapacity(
    val quantity: Long = 1L,
    override val correlationId: CommandCorrelationId? = null
) : InventoryCommand

/** Move one exact overflow-owned item into normal inventory without rerolling it. */
data class ClaimOverflowItem(
    val itemInstanceId: InstanceId,
    override val correlationId: CommandCorrelationId? = null
) : InventoryCommand

/** Salvage one exact overflow-owned item through the normal salvage economy. */
data class SalvageOverflowItem(
    val itemInstanceId: InstanceId,
    override val correlationId: CommandCorrelationId? = null
) : InventoryCommand

/** Atomically salvages an explicit overflow selection in stable InstanceId order. */
data class SalvageOverflowItems(
    val itemInstanceIds: Set<InstanceId>,
    override val correlationId: CommandCorrelationId? = null
) : InventoryCommand {
    init { require(itemInstanceIds.isNotEmpty()) }
}
