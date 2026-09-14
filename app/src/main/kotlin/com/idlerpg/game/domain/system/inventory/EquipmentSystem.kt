package com.idlerpg.game.domain.system.inventory

import com.idlerpg.game.core.id.InstanceId
import com.idlerpg.game.data.content.ContentRegistry
import com.idlerpg.game.domain.command.CommandRejectionCode
import com.idlerpg.game.domain.command.CommandRejectionReason
import com.idlerpg.game.domain.definition.EquipmentSlot
import com.idlerpg.game.domain.event.GameEvent
import com.idlerpg.game.domain.event.ItemEquipped
import com.idlerpg.game.domain.event.ItemUnequipped
import com.idlerpg.game.domain.model.inventory.EquipmentLoadoutState
import com.idlerpg.game.domain.model.inventory.InventoryState

/** Pure equipment transition result. */
sealed interface EquipmentOperationResult {
    data class Accepted(
        val inventory: InventoryState,
        val events: List<GameEvent>
    ) : EquipmentOperationResult

    data class Rejected(
        val reason: CommandRejectionReason
    ) : EquipmentOperationResult
}

/**
 * Owns equipment reference changes.
 *
 * Equipped items remain in InventoryState.itemsById. Foundation 11 intentionally applies
 * no stat modifiers yet; mechanical equipment effects begin in Foundation 12.
 */
object EquipmentSystem {

    fun equip(
        inventory: InventoryState,
        itemInstanceId: InstanceId,
        slot: EquipmentSlot,
        contentRegistry: ContentRegistry
    ): EquipmentOperationResult {
        val item = inventory.itemsById[itemInstanceId]
            ?: return rejected(
                code = CommandRejectionCode.NOT_OWNED,
                itemInstanceId = itemInstanceId
            )
        val itemDefinition = contentRegistry.item(item.definitionId)
        val equipmentDefinitionId =
            itemDefinition.equipmentDefinitionId
                ?: return rejected(
                    code = CommandRejectionCode.INVALID_ARGUMENT,
                    itemInstanceId = itemInstanceId
                )
        val equipmentDefinition =
            contentRegistry.equipment(equipmentDefinitionId)

        if (equipmentDefinition.slot != slot) {
            return rejected(
                code = CommandRejectionCode.INVALID_ARGUMENT,
                itemInstanceId = itemInstanceId
            )
        }

        val currentlyIn = inventory.equipment.slotOf(itemInstanceId)
        if (currentlyIn != null) {
            return rejected(
                code = CommandRejectionCode.INVALID_STATE,
                itemInstanceId = itemInstanceId
            )
        }

        val displaced = inventory.equipment.itemIn(slot)
        val newMap = inventory.equipment.itemBySlot.toMutableMap()
        newMap[slot] = itemInstanceId

        val newInventory = inventory.copy(
            equipment = EquipmentLoadoutState(
                itemBySlot = newMap.toMap()
            )
        )

        return EquipmentOperationResult.Accepted(
            inventory = newInventory,
            events = buildList {
                if (displaced != null) {
                    add(
                        ItemUnequipped(
                            itemInstanceId = displaced,
                            slot = slot
                        )
                    )
                }
                add(
                    ItemEquipped(
                        itemInstanceId = itemInstanceId,
                        slot = slot
                    )
                )
            }
        )
    }

    fun unequip(
        inventory: InventoryState,
        slot: EquipmentSlot
    ): EquipmentOperationResult {
        val itemInstanceId =
            inventory.equipment.itemIn(slot)
                ?: return EquipmentOperationResult.Rejected(
                    CommandRejectionReason(
                        code = CommandRejectionCode.INVALID_STATE
                    )
                )

        val newMap = inventory.equipment.itemBySlot.toMutableMap()
        newMap.remove(slot)

        return EquipmentOperationResult.Accepted(
            inventory = inventory.copy(
                equipment = EquipmentLoadoutState(
                    itemBySlot = newMap.toMap()
                )
            ),
            events = listOf(
                ItemUnequipped(
                    itemInstanceId = itemInstanceId,
                    slot = slot
                )
            )
        )
    }

    private fun rejected(
        code: CommandRejectionCode,
        itemInstanceId: InstanceId
    ): EquipmentOperationResult.Rejected =
        EquipmentOperationResult.Rejected(
            CommandRejectionReason(
                code = code,
                subjectInstanceId = itemInstanceId
            )
        )
}
