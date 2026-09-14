package com.idlerpg.game.presentation

import com.idlerpg.game.core.id.InstanceId
import com.idlerpg.game.domain.command.ClaimOverflowItem
import com.idlerpg.game.domain.command.CommandCorrelationId
import com.idlerpg.game.domain.command.EquipItem
import com.idlerpg.game.domain.command.ExpandInventoryCapacity
import com.idlerpg.game.domain.command.LockItem
import com.idlerpg.game.domain.command.SalvageItem
import com.idlerpg.game.domain.command.SalvageOverflowItem
import com.idlerpg.game.domain.command.UnequipItem
import com.idlerpg.game.domain.command.UnlockItem
import com.idlerpg.game.domain.definition.EquipmentSlot
import com.idlerpg.game.presentation.intent.GearUiIntent
import com.idlerpg.game.presentation.intent.toGameCommand

object GearIntentMappingTest {
    fun run() {
        val correlationId = CommandCorrelationId(707L)
        val itemId = InstanceId(77L)

        check(
            GearUiIntent.Equip(itemId, EquipmentSlot.WEAPON).toGameCommand(correlationId) ==
                EquipItem(itemId, EquipmentSlot.WEAPON, correlationId)
        )
        check(
            GearUiIntent.Unequip(EquipmentSlot.WEAPON).toGameCommand(correlationId) ==
                UnequipItem(EquipmentSlot.WEAPON, correlationId)
        )
        check(
            GearUiIntent.SetLocked(itemId, true).toGameCommand(correlationId) ==
                LockItem(itemId, correlationId)
        )
        check(
            GearUiIntent.SetLocked(itemId, false).toGameCommand(correlationId) ==
                UnlockItem(itemId, correlationId)
        )
        check(
            GearUiIntent.Salvage(itemId).toGameCommand(correlationId) ==
                SalvageItem(itemId, correlationId)
        )
        check(
            GearUiIntent.ExpandCapacity.toGameCommand(correlationId) ==
                ExpandInventoryCapacity(1L, correlationId)
        )
        check(
            GearUiIntent.ClaimOverflow(itemId).toGameCommand(correlationId) ==
                ClaimOverflowItem(itemId, correlationId)
        )
        check(
            GearUiIntent.SalvageOverflow(itemId).toGameCommand(correlationId) ==
                SalvageOverflowItem(itemId, correlationId)
        )

        println("FUI07_GEAR_INTENT_MAPPING_PASS")
    }
}
