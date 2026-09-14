package com.idlerpg.game.domain.model.inventory

import com.idlerpg.game.core.id.InstanceId
import com.idlerpg.game.domain.definition.EquipmentSlot

/**
 * Equipped-item references.
 *
 * Equipped items remain owned by InventoryState; this mapping never owns a second copy of
 * an ItemInstance.
 */
data class EquipmentLoadoutState(
    val itemBySlot: Map<EquipmentSlot, InstanceId> = emptyMap()
) {
    init {
        require(itemBySlot.values.size == itemBySlot.values.toSet().size) {
            "One item instance cannot occupy multiple equipment slots"
        }
    }

    fun itemIn(slot: EquipmentSlot): InstanceId? = itemBySlot[slot]

    fun slotOf(itemInstanceId: InstanceId): EquipmentSlot? =
        itemBySlot.entries.firstOrNull { it.value == itemInstanceId }?.key
}
