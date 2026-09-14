package com.idlerpg.game.domain.system.inventory

import com.idlerpg.game.core.config.BalanceConfig
import com.idlerpg.game.core.number.GameNumber
import com.idlerpg.game.domain.command.CommandRejectionCode
import com.idlerpg.game.domain.command.CommandRejectionReason
import com.idlerpg.game.domain.command.ExpandInventoryCapacity
import com.idlerpg.game.domain.definition.CurrencyId
import com.idlerpg.game.domain.engine.CommandHandlingResult
import com.idlerpg.game.domain.engine.EngineContext
import com.idlerpg.game.domain.event.CurrencySpent
import com.idlerpg.game.domain.event.InventoryCapacityExpanded
import com.idlerpg.game.domain.event.InventoryProgressionUnblocked
import com.idlerpg.game.domain.model.GameState
import com.idlerpg.game.domain.model.inventory.InventoryState
import com.idlerpg.game.domain.system.economy.TransactionSystem

/** Canonical inventory-capacity, expansion-cost, and storage-headroom owner. */
object InventoryCapacitySystem {

    /**
     * FBE-00 through FBE-03 could create V2 saves with more normal items than the then-
     * unenforced stored capacity. Those items are grandfathered rather than deleted or
     * migrated to overflow. The first accepted inventory/capacity mutation persists this
     * effective capacity so the save self-normalizes without a Save V3 schema change.
     */
    fun normalizeGrandfatheredCapacity(inventory: InventoryState): InventoryState {
        val effective = maxOf(inventory.slotCapacity, inventory.itemsById.size.toLong())
        return if (effective == inventory.slotCapacity) inventory
        else inventory.copy(slotCapacity = effective)
    }

    fun effectiveSlotCapacity(inventory: InventoryState): Long =
        maxOf(inventory.slotCapacity, inventory.itemsById.size.toLong())

    fun normalSlotsAvailable(inventory: InventoryState): Long =
        (effectiveSlotCapacity(inventory) - inventory.itemsById.size.toLong())
            .coerceAtLeast(0L)

    fun overflowSlotsAvailable(
        inventory: InventoryState,
        balanceConfig: BalanceConfig
    ): Long =
        (balanceConfig.inventoryOverflowCapacity - inventory.overflowItemsById.size.toLong())
            .coerceAtLeast(0L)

    fun totalStorageSlotsAvailable(
        inventory: InventoryState,
        balanceConfig: BalanceConfig
    ): Long = saturatedAdd(
        normalSlotsAvailable(inventory),
        overflowSlotsAvailable(inventory, balanceConfig)
    )

    fun isProgressionBlocked(
        inventory: InventoryState,
        balanceConfig: BalanceConfig
    ): Boolean = false

    fun nextExpansionCost(
        inventory: InventoryState,
        balanceConfig: BalanceConfig
    ): GameNumber = expansionCostAtPurchaseIndex(
        purchaseIndex = inventory.capacityUpgradePurchases,
        balanceConfig = balanceConfig
    )

    fun totalExpansionCost(
        inventory: InventoryState,
        quantity: Long,
        balanceConfig: BalanceConfig
    ): GameNumber {
        require(quantity > 0L) { "Expansion quantity must be positive" }
        var total = GameNumber.ZERO
        var offset = 0L
        while (offset < quantity) {
            total += expansionCostAtPurchaseIndex(
                purchaseIndex = Math.addExact(inventory.capacityUpgradePurchases, offset),
                balanceConfig = balanceConfig
            )
            offset += 1L
        }
        return total
    }

    fun handleExpand(
        state: GameState,
        command: ExpandInventoryCapacity,
        context: EngineContext
    ): CommandHandlingResult {
        if (command.quantity <= 0L) {
            return rejected(CommandRejectionCode.INVALID_ARGUMENT)
        }

        val balance = context.balanceConfig
        val normalized = normalizeGrandfatheredCapacity(state.run.inventory)
        val previousCapacity = normalized.slotCapacity

        val capacityIncrease = try {
            Math.multiplyExact(balance.inventoryCapacityPerExpansion, command.quantity)
        } catch (_: ArithmeticException) {
            return rejected(CommandRejectionCode.CAPACITY_EXCEEDED)
        }
        val newCapacity = try {
            Math.addExact(previousCapacity, capacityIncrease)
        } catch (_: ArithmeticException) {
            return rejected(CommandRejectionCode.CAPACITY_EXCEEDED)
        }

        if (previousCapacity >= balance.maximumInventoryCapacity ||
            newCapacity > balance.maximumInventoryCapacity
        ) {
            return rejected(CommandRejectionCode.CAPACITY_EXCEEDED)
        }

        val totalCost = try {
            totalExpansionCost(normalized, command.quantity, balance)
        } catch (_: ArithmeticException) {
            // A malformed/legacy purchase counter must be rejected atomically rather than
            // taking down the command worker while calculating the next authored price.
            return rejected(CommandRejectionCode.CAPACITY_EXCEEDED)
        }
        val newEconomy = TransactionSystem.spend(
            economy = state.run.economy,
            currencyId = CurrencyId.GOLD,
            amount = totalCost
        ) ?: return rejected(CommandRejectionCode.INSUFFICIENT_RESOURCE)

        val beforeBlocked = isProgressionBlocked(normalized, balance)
        val nextPurchaseCount = try {
            Math.addExact(normalized.capacityUpgradePurchases, command.quantity)
        } catch (_: ArithmeticException) {
            return rejected(CommandRejectionCode.CAPACITY_EXCEEDED)
        }
        val newInventory = normalized.copy(
            slotCapacity = newCapacity,
            capacityUpgradePurchases = nextPurchaseCount
        )
        val availableAfter = totalStorageSlotsAvailable(newInventory, balance)

        return CommandHandlingResult.Accepted(
            state = state.copy(
                run = state.run.copy(
                    inventory = newInventory,
                    economy = newEconomy
                )
            ),
            events = buildList {
                add(CurrencySpent(CurrencyId.GOLD, totalCost, purposeId = null))
                add(
                    InventoryCapacityExpanded(
                        quantity = command.quantity,
                        previousCapacity = previousCapacity,
                        newCapacity = newCapacity,
                        totalGoldCost = totalCost
                    )
                )
                if (beforeBlocked && availableAfter > 0L) {
                    add(InventoryProgressionUnblocked(availableAfter))
                }
            }
        )
    }

    private fun expansionCostAtPurchaseIndex(
        purchaseIndex: Long,
        balanceConfig: BalanceConfig
    ): GameNumber {
        require(purchaseIndex >= 0L) { "purchaseIndex cannot be negative" }
        return balanceConfig.baseInventoryExpansionGoldCost +
            (balanceConfig.inventoryExpansionGoldCostStep * purchaseIndex)
    }

    /** Headroom is a diagnostic/projection value; it must not crash on a legacy extreme. */
    private fun saturatedAdd(left: Long, right: Long): Long =
        if (Long.MAX_VALUE - left < right) Long.MAX_VALUE else left + right

    private fun rejected(code: CommandRejectionCode): CommandHandlingResult.Rejected =
        CommandHandlingResult.Rejected(CommandRejectionReason(code = code))
}
