package com.idlerpg.game.domain.model.inventory

import com.idlerpg.game.core.config.BalanceConfig
import com.idlerpg.game.core.id.InstanceId

/**
 * Canonical run-level item ownership state.
 *
 * Equipped items remain in [itemsById], while [equipment] stores only instance-ID
 * references. Overflow is a separate ownership container whose items cannot be equipped
 * or locked until a later backend command transfers them into normal inventory.
 *
 * FBE-04 routes every newly generated item through bounded normal/overflow storage.
 * The model intentionally remains tolerant of pre-FBE-04 V2 saves whose normal item count
 * already exceeds the stored capacity; InventoryCapacitySystem grandfathers and normalizes
 * that historical overage on the first accepted inventory/capacity mutation.
 */
data class InventoryState(
    val itemsById: Map<InstanceId, ItemInstance> = emptyMap(),
    val equipment: EquipmentLoadoutState = EquipmentLoadoutState(),
    val locks: ItemLockState = ItemLockState(),
    val slotCapacity: Long = BalanceConfig.DEFAULT_BASE_INVENTORY_CAPACITY,
    val capacityUpgradePurchases: Long = 0L,
    val overflowItemsById: Map<InstanceId, ItemInstance> = emptyMap(),
    val lootFilter: LootFilterState = LootFilterState()
) {
    val ownedItemInstanceIds: Set<InstanceId>
        get() = itemsById.keys

    val overflowItemInstanceIds: Set<InstanceId>
        get() = overflowItemsById.keys

    init {
        require(slotCapacity > 0L) {
            "Inventory slotCapacity must be positive: $slotCapacity"
        }
        require(capacityUpgradePurchases >= 0L) {
            "Inventory capacityUpgradePurchases cannot be negative: $capacityUpgradePurchases"
        }
        require(itemsById.entries.all { (id, item) -> id == item.instanceId }) {
            "InventoryState map key must equal ItemInstance.instanceId"
        }
        require(overflowItemsById.entries.all { (id, item) -> id == item.instanceId }) {
            "InventoryState overflow map key must equal ItemInstance.instanceId"
        }
        require(itemsById.keys.intersect(overflowItemsById.keys).isEmpty()) {
            "Normal inventory and overflow cannot own the same ItemInstance"
        }
        require(equipment.itemBySlot.values.all { it in itemsById }) {
            "Every equipped item instance must be owned by the normal inventory"
        }
        require(locks.lockedItemInstanceIds.all { it in itemsById }) {
            "Every locked item instance must be owned by the normal inventory"
        }
    }

    fun item(itemInstanceId: InstanceId): ItemInstance? =
        itemsById[itemInstanceId]

    fun overflowItem(itemInstanceId: InstanceId): ItemInstance? =
        overflowItemsById[itemInstanceId]
}
