package com.idlerpg.game.presentation.intent

import com.idlerpg.game.core.id.InstanceId
import com.idlerpg.game.domain.command.ClaimOverflowItem
import com.idlerpg.game.domain.command.CommandCorrelationId
import com.idlerpg.game.domain.command.EquipItem
import com.idlerpg.game.domain.command.ExpandInventoryCapacity
import com.idlerpg.game.domain.command.GameCommand
import com.idlerpg.game.domain.command.LockItem
import com.idlerpg.game.domain.command.SalvageItem
import com.idlerpg.game.domain.command.SalvageItems
import com.idlerpg.game.domain.command.SalvageAllBelow
import com.idlerpg.game.domain.command.ConfigureLootFilter
import com.idlerpg.game.domain.command.SalvageOverflowItem
import com.idlerpg.game.domain.command.SalvageOverflowItems
import com.idlerpg.game.domain.command.UnequipItem
import com.idlerpg.game.domain.command.UnlockItem
import com.idlerpg.game.domain.definition.EquipmentSlot
import com.idlerpg.game.domain.definition.Rarity

/** FUI-07 presentation intents. Every mutation maps to exactly one canonical inventory command. */
sealed interface GearUiIntent {
    data class Equip(
        val itemInstanceId: InstanceId,
        val slot: EquipmentSlot
    ) : GearUiIntent

    data class Unequip(
        val slot: EquipmentSlot
    ) : GearUiIntent

    data class SetLocked(
        val itemInstanceId: InstanceId,
        val locked: Boolean
    ) : GearUiIntent

    data class Salvage(
        val itemInstanceId: InstanceId
    ) : GearUiIntent

    data class SalvageSelected(val itemInstanceIds: Set<InstanceId>) : GearUiIntent
    data class SalvageBelow(val rarity: Rarity) : GearUiIntent

    data class SetLootFilter(
        val enabled: Boolean,
        val minimumKeepRarity: Rarity
    ) : GearUiIntent

    data object ExpandCapacity : GearUiIntent

    data class ClaimOverflow(
        val itemInstanceId: InstanceId
    ) : GearUiIntent

    data class SalvageOverflow(
        val itemInstanceId: InstanceId
    ) : GearUiIntent

    data class SalvageOverflowSelected(val itemInstanceIds: Set<InstanceId>) : GearUiIntent
}

fun GearUiIntent.toGameCommand(
    correlationId: CommandCorrelationId
): GameCommand = when (this) {
    is GearUiIntent.Equip -> EquipItem(
        itemInstanceId = itemInstanceId,
        slot = slot,
        correlationId = correlationId
    )
    is GearUiIntent.Unequip -> UnequipItem(
        slot = slot,
        correlationId = correlationId
    )
    is GearUiIntent.SetLocked -> if (locked) {
        LockItem(
            itemInstanceId = itemInstanceId,
            correlationId = correlationId
        )
    } else {
        UnlockItem(
            itemInstanceId = itemInstanceId,
            correlationId = correlationId
        )
    }
    is GearUiIntent.Salvage -> SalvageItem(
        itemInstanceId = itemInstanceId,
        correlationId = correlationId
    )
    is GearUiIntent.SalvageSelected -> SalvageItems(itemInstanceIds, correlationId)
    is GearUiIntent.SalvageBelow -> SalvageAllBelow(rarity, correlationId)
    is GearUiIntent.SetLootFilter -> ConfigureLootFilter(
        autoSalvageEnabled = enabled,
        minimumKeepRarity = minimumKeepRarity,
        correlationId = correlationId
    )
    GearUiIntent.ExpandCapacity -> ExpandInventoryCapacity(
        quantity = 1L,
        correlationId = correlationId
    )
    is GearUiIntent.ClaimOverflow -> ClaimOverflowItem(
        itemInstanceId = itemInstanceId,
        correlationId = correlationId
    )
    is GearUiIntent.SalvageOverflow -> SalvageOverflowItem(
        itemInstanceId = itemInstanceId,
        correlationId = correlationId
    )
    is GearUiIntent.SalvageOverflowSelected -> SalvageOverflowItems(
        itemInstanceIds,
        correlationId
    )
}
