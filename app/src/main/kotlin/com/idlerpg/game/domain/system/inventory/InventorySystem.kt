package com.idlerpg.game.domain.system.inventory

import com.idlerpg.game.core.config.BalanceConfig
import com.idlerpg.game.core.id.InstanceId
import com.idlerpg.game.core.number.GameNumber
import com.idlerpg.game.domain.command.ClaimOverflowItem
import com.idlerpg.game.domain.command.CommandRejectionCode
import com.idlerpg.game.domain.command.CommandRejectionReason
import com.idlerpg.game.domain.command.EquipItem
import com.idlerpg.game.domain.command.ExpandInventoryCapacity
import com.idlerpg.game.domain.command.GameCommand
import com.idlerpg.game.domain.command.InventoryCommand
import com.idlerpg.game.domain.command.LockItem
import com.idlerpg.game.domain.command.SalvageItem
import com.idlerpg.game.domain.command.SalvageItems
import com.idlerpg.game.domain.command.SalvageAllBelow
import com.idlerpg.game.domain.command.ConfigureLootFilter
import com.idlerpg.game.domain.command.SalvageOverflowItem
import com.idlerpg.game.domain.command.SalvageOverflowItems
import com.idlerpg.game.domain.command.UnequipItem
import com.idlerpg.game.domain.command.UnlockItem
import com.idlerpg.game.domain.definition.CurrencyId
import com.idlerpg.game.domain.engine.CommandHandlingResult
import com.idlerpg.game.domain.engine.EngineContext
import com.idlerpg.game.domain.engine.GameCommandHandler
import com.idlerpg.game.domain.event.CurrencyGranted
import com.idlerpg.game.domain.event.GameEvent
import com.idlerpg.game.domain.event.InventoryProgressionUnblocked
import com.idlerpg.game.domain.event.ItemLocked
import com.idlerpg.game.domain.event.ItemSalvaged
import com.idlerpg.game.domain.event.ItemUnlocked
import com.idlerpg.game.domain.event.OverflowItemClaimed
import com.idlerpg.game.domain.event.OverflowItemSalvaged
import com.idlerpg.game.domain.event.LootFilterConfigured
import com.idlerpg.game.domain.model.GameState
import com.idlerpg.game.domain.model.inventory.InventoryState
import com.idlerpg.game.domain.model.inventory.ItemInstance
import com.idlerpg.game.domain.model.inventory.ItemLockState
import com.idlerpg.game.domain.model.inventory.LootFilterState
import com.idlerpg.game.domain.system.economy.TransactionSystem

enum class GeneratedItemPlacement { NORMAL, OVERFLOW }

data class GeneratedItemAddResult(
    val inventory: InventoryState,
    val placement: GeneratedItemPlacement
)

/** Canonical inventory ownership and manual inventory-command coordinator. */
object InventorySystem : GameCommandHandler {

    override fun handle(
        state: GameState,
        command: GameCommand,
        context: EngineContext
    ): CommandHandlingResult = when (command) {
        is EquipItem -> fromEquipmentResult(
            state,
            EquipmentSystem.equip(
                state.run.inventory,
                command.itemInstanceId,
                command.slot,
                context.contentRegistry
            )
        )
        is UnequipItem -> fromEquipmentResult(
            state,
            EquipmentSystem.unequip(state.run.inventory, command.slot)
        )
        is LockItem -> setLocked(state, command.itemInstanceId, true)
        is UnlockItem -> setLocked(state, command.itemInstanceId, false)
        is SalvageItem -> salvage(state, command.itemInstanceId, context)
        is SalvageItems -> salvageMany(state, command.itemInstanceIds, context)
        is SalvageAllBelow -> salvageMany(
            state,
            state.run.inventory.itemsById.values
                .filter { item -> item.rarity.rank < command.rarity.rank &&
                    item.instanceId !in state.run.inventory.locks.lockedItemInstanceIds &&
                    item.instanceId !in state.run.inventory.equipment.itemBySlot.values }
                .map { it.instanceId }.toSet(),
            context,
            allowEmpty = true
        )
        is ConfigureLootFilter -> configureLootFilter(state, command)
        is ExpandInventoryCapacity ->
            InventoryCapacitySystem.handleExpand(state, command, context)
        is ClaimOverflowItem -> claimOverflow(state, command.itemInstanceId)
        is SalvageOverflowItem -> salvageOverflow(state, command.itemInstanceId, context)
        is SalvageOverflowItems -> salvageOverflowMany(state, command.itemInstanceIds, context)
        is InventoryCommand -> rejected(CommandRejectionCode.UNSUPPORTED)
        else -> rejected(CommandRejectionCode.UNSUPPORTED)
    }

    /** Ownership transfer for kept generated items; full soft overflow remains lossless. */
    fun addGeneratedItem(
        inventory: InventoryState,
        item: ItemInstance,
        balanceConfig: BalanceConfig
    ): GeneratedItemAddResult {
        val normalized = InventoryCapacitySystem.normalizeGrandfatheredCapacity(inventory)
        require(item.instanceId !in normalized.itemsById &&
            item.instanceId !in normalized.overflowItemsById
        ) { "Duplicate ItemInstance ID: ${item.instanceId}" }

        if (InventoryCapacitySystem.normalSlotsAvailable(normalized) > 0L) {
            return GeneratedItemAddResult(
                inventory = normalized.copy(
                    itemsById = (normalized.itemsById + (item.instanceId to item)).toSortedMap()
                ),
                placement = GeneratedItemPlacement.NORMAL
            )
        }

        if (InventoryCapacitySystem.overflowSlotsAvailable(normalized, balanceConfig) > 0L) {
            return GeneratedItemAddResult(
                inventory = normalized.copy(
                    overflowItemsById =
                        (normalized.overflowItemsById + (item.instanceId to item)).toSortedMap()
                ),
                placement = GeneratedItemPlacement.OVERFLOW
            )
        }

        // Kept items are Rare+ by default. Preserve them in deterministic overflow even
        // beyond the soft display capacity so unattended simulation never destroys value.
        return GeneratedItemAddResult(
            inventory = normalized.copy(
                overflowItemsById =
                    (normalized.overflowItemsById + (item.instanceId to item)).toSortedMap()
            ),
            placement = GeneratedItemPlacement.OVERFLOW
        )
    }

    private fun configureLootFilter(
        state: GameState,
        command: ConfigureLootFilter
    ): CommandHandlingResult.Accepted {
        val filter = LootFilterState(command.autoSalvageEnabled, command.minimumKeepRarity)
        return CommandHandlingResult.Accepted(
            state.copy(run = state.run.copy(inventory = state.run.inventory.copy(lootFilter = filter))),
            listOf(LootFilterConfigured(filter.autoSalvageEnabled, filter.minimumKeepRarity))
        )
    }

    private fun salvageMany(
        state: GameState,
        itemIds: Set<InstanceId>,
        context: EngineContext,
        allowEmpty: Boolean = false
    ): CommandHandlingResult {
        if (itemIds.isEmpty()) {
            return if (allowEmpty) CommandHandlingResult.Accepted(state, emptyList())
            else rejected(CommandRejectionCode.INVALID_ARGUMENT)
        }
        val inventory = InventoryCapacitySystem.normalizeGrandfatheredCapacity(state.run.inventory)
        val ordered = itemIds.sorted()
        for (id in ordered) {
            if (id !in inventory.itemsById) return rejected(CommandRejectionCode.NOT_OWNED, id)
            if (inventory.locks.isLocked(id)) return rejected(CommandRejectionCode.LOCKED, id)
            if (inventory.equipment.slotOf(id) != null) return rejected(CommandRejectionCode.INVALID_STATE, id)
        }
        var totalGold = GameNumber.ZERO
        val events = mutableListOf<GameEvent>()
        ordered.forEach { id ->
            val item = inventory.itemsById.getValue(id)
            val gold = context.contentRegistry.item(item.definitionId).salvageProfile.goldFor(item.rarity)
            totalGold += gold
            events += ItemSalvaged(id, item.definitionId, gold)
        }
        val nextInventory = inventory.copy(itemsById = inventory.itemsById.filterKeys { it !in itemIds }.toSortedMap())
        val nextEconomy = if (totalGold > GameNumber.ZERO) {
            events += CurrencyGranted(CurrencyId.GOLD, totalGold, null)
            TransactionSystem.grant(state.run.economy, CurrencyId.GOLD, totalGold)
        } else state.run.economy
        return CommandHandlingResult.Accepted(
            state.copy(run = state.run.copy(inventory = nextInventory, economy = nextEconomy)),
            events
        )
    }

    private fun fromEquipmentResult(
        state: GameState,
        result: EquipmentOperationResult
    ): CommandHandlingResult = when (result) {
        is EquipmentOperationResult.Rejected -> CommandHandlingResult.Rejected(result.reason)
        is EquipmentOperationResult.Accepted -> CommandHandlingResult.Accepted(
            state = state.copy(run = state.run.copy(inventory = result.inventory)),
            events = result.events
        )
    }

    private fun setLocked(
        state: GameState,
        itemInstanceId: InstanceId,
        locked: Boolean
    ): CommandHandlingResult {
        val inventory = state.run.inventory
        if (itemInstanceId !in inventory.itemsById) {
            return rejected(CommandRejectionCode.NOT_OWNED, itemInstanceId)
        }
        val currentlyLocked = itemInstanceId in inventory.locks.lockedItemInstanceIds
        if (currentlyLocked == locked) {
            return rejected(CommandRejectionCode.INVALID_STATE, itemInstanceId)
        }
        val newLocks = if (locked) {
            inventory.locks.lockedItemInstanceIds + itemInstanceId
        } else {
            inventory.locks.lockedItemInstanceIds - itemInstanceId
        }
        val newInventory = inventory.copy(
            locks = ItemLockState(lockedItemInstanceIds = newLocks)
        )
        val event: GameEvent = if (locked) ItemLocked(itemInstanceId) else ItemUnlocked(itemInstanceId)
        return CommandHandlingResult.Accepted(
            state.copy(run = state.run.copy(inventory = newInventory)),
            listOf(event)
        )
    }

    private fun salvage(
        state: GameState,
        itemInstanceId: InstanceId,
        context: EngineContext
    ): CommandHandlingResult {
        val inventory = InventoryCapacitySystem.normalizeGrandfatheredCapacity(state.run.inventory)
        val item = inventory.itemsById[itemInstanceId]
            ?: return rejected(CommandRejectionCode.NOT_OWNED, itemInstanceId)
        if (inventory.locks.isLocked(itemInstanceId)) {
            return rejected(CommandRejectionCode.LOCKED, itemInstanceId)
        }
        if (inventory.equipment.slotOf(itemInstanceId) != null) {
            return rejected(CommandRejectionCode.INVALID_STATE, itemInstanceId)
        }

        val beforeBlocked = InventoryCapacitySystem.isProgressionBlocked(inventory, context.balanceConfig)
        val definition = context.contentRegistry.item(item.definitionId)
        val gold = definition.salvageProfile.goldFor(item.rarity)
        val newInventory = inventory.copy(
            itemsById = inventory.itemsById.filterKeys { it != itemInstanceId }.toSortedMap()
        )
        val newEconomy = if (gold == GameNumber.ZERO) state.run.economy else
            TransactionSystem.grant(state.run.economy, CurrencyId.GOLD, gold)
        val availableAfter = InventoryCapacitySystem.totalStorageSlotsAvailable(
            newInventory,
            context.balanceConfig
        )

        return CommandHandlingResult.Accepted(
            state = state.copy(run = state.run.copy(inventory = newInventory, economy = newEconomy)),
            events = buildList {
                add(ItemSalvaged(item.instanceId, item.definitionId, gold))
                if (gold > GameNumber.ZERO) {
                    add(CurrencyGranted(CurrencyId.GOLD, gold, item.definitionId))
                }
                if (beforeBlocked && availableAfter > 0L) {
                    add(InventoryProgressionUnblocked(availableAfter))
                }
            }
        )
    }

    private fun claimOverflow(
        state: GameState,
        itemInstanceId: InstanceId
    ): CommandHandlingResult {
        val inventory = InventoryCapacitySystem.normalizeGrandfatheredCapacity(state.run.inventory)
        val item = inventory.overflowItemsById[itemInstanceId]
            ?: return rejected(CommandRejectionCode.NOT_OWNED, itemInstanceId)
        if (InventoryCapacitySystem.normalSlotsAvailable(inventory) <= 0L) {
            return rejected(CommandRejectionCode.CAPACITY_EXCEEDED, itemInstanceId)
        }
        val next = inventory.copy(
            itemsById = (inventory.itemsById + (itemInstanceId to item)).toSortedMap(),
            overflowItemsById = inventory.overflowItemsById
                .filterKeys { it != itemInstanceId }
                .toSortedMap()
        )
        return CommandHandlingResult.Accepted(
            state.copy(run = state.run.copy(inventory = next)),
            listOf(OverflowItemClaimed(item.instanceId, item.definitionId))
        )
    }

    private fun salvageOverflow(
        state: GameState,
        itemInstanceId: InstanceId,
        context: EngineContext
    ): CommandHandlingResult {
        val inventory = InventoryCapacitySystem.normalizeGrandfatheredCapacity(state.run.inventory)
        val item = inventory.overflowItemsById[itemInstanceId]
            ?: return rejected(CommandRejectionCode.NOT_OWNED, itemInstanceId)
        val beforeBlocked = InventoryCapacitySystem.isProgressionBlocked(inventory, context.balanceConfig)
        val definition = context.contentRegistry.item(item.definitionId)
        val gold = definition.salvageProfile.goldFor(item.rarity)
        val nextInventory = inventory.copy(
            overflowItemsById = inventory.overflowItemsById
                .filterKeys { it != itemInstanceId }
                .toSortedMap()
        )
        val nextEconomy = if (gold == GameNumber.ZERO) state.run.economy else
            TransactionSystem.grant(state.run.economy, CurrencyId.GOLD, gold)
        val availableAfter = InventoryCapacitySystem.totalStorageSlotsAvailable(
            nextInventory,
            context.balanceConfig
        )
        return CommandHandlingResult.Accepted(
            state.copy(run = state.run.copy(inventory = nextInventory, economy = nextEconomy)),
            buildList {
                add(OverflowItemSalvaged(item.instanceId, item.definitionId, gold))
                if (gold > GameNumber.ZERO) {
                    add(CurrencyGranted(CurrencyId.GOLD, gold, item.definitionId))
                }
                if (beforeBlocked && availableAfter > 0L) {
                    add(InventoryProgressionUnblocked(availableAfter))
                }
            }
        )
    }

    private fun salvageOverflowMany(
        state: GameState,
        itemIds: Set<InstanceId>,
        context: EngineContext
    ): CommandHandlingResult {
        val inventory = InventoryCapacitySystem.normalizeGrandfatheredCapacity(state.run.inventory)
        val ordered = itemIds.sorted()
        if (ordered.isEmpty()) {
            return rejected(CommandRejectionCode.INVALID_ARGUMENT)
        }
        if (ordered.any { it !in inventory.overflowItemsById }) {
            return rejected(CommandRejectionCode.NOT_OWNED, ordered.firstOrNull())
        }
        var totalGold = GameNumber.ZERO
        val events = mutableListOf<GameEvent>()
        ordered.forEach { id ->
            val item = inventory.overflowItemsById.getValue(id)
            val gold = context.contentRegistry.item(item.definitionId).salvageProfile.goldFor(item.rarity)
            totalGold += gold
            events += OverflowItemSalvaged(id, item.definitionId, gold)
        }
        val economy = if (totalGold > GameNumber.ZERO) {
            events += CurrencyGranted(CurrencyId.GOLD, totalGold, null)
            TransactionSystem.grant(state.run.economy, CurrencyId.GOLD, totalGold)
        } else state.run.economy
        return CommandHandlingResult.Accepted(
            state.copy(run = state.run.copy(
                inventory = inventory.copy(
                    overflowItemsById = inventory.overflowItemsById.filterKeys { it !in itemIds }.toSortedMap()
                ),
                economy = economy
            )),
            events
        )
    }

    private fun rejected(
        code: CommandRejectionCode,
        itemInstanceId: InstanceId? = null
    ): CommandHandlingResult.Rejected = CommandHandlingResult.Rejected(
        CommandRejectionReason(code = code, subjectInstanceId = itemInstanceId)
    )
}
