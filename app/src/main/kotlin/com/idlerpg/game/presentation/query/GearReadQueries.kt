package com.idlerpg.game.presentation.query

import com.idlerpg.game.core.config.BalanceConfig
import com.idlerpg.game.core.number.GameNumber
import com.idlerpg.game.domain.model.GameState
import com.idlerpg.game.domain.system.inventory.InventoryCapacitySystem

/** FUI-07 read-only facade over backend-owned inventory capacity and pricing rules. */
interface GearReadQueries {
    fun effectiveNormalCapacity(state: GameState): Long
    fun normalSlotsAvailable(state: GameState): Long
    fun overflowCapacity(): Long
    fun overflowSlotsAvailable(state: GameState): Long
    fun capacityPerExpansion(): Long
    fun maximumCapacity(): Long
    fun nextExpansionCost(state: GameState): GameNumber
    fun progressionBlocked(state: GameState): Boolean
}

class DefaultGearReadQueries(
    private val balanceConfig: BalanceConfig
) : GearReadQueries {
    override fun effectiveNormalCapacity(state: GameState): Long =
        InventoryCapacitySystem.effectiveSlotCapacity(state.run.inventory)

    override fun normalSlotsAvailable(state: GameState): Long =
        InventoryCapacitySystem.normalSlotsAvailable(state.run.inventory)

    override fun overflowCapacity(): Long = balanceConfig.inventoryOverflowCapacity

    override fun overflowSlotsAvailable(state: GameState): Long =
        InventoryCapacitySystem.overflowSlotsAvailable(state.run.inventory, balanceConfig)

    override fun capacityPerExpansion(): Long = balanceConfig.inventoryCapacityPerExpansion

    override fun maximumCapacity(): Long = balanceConfig.maximumInventoryCapacity

    override fun nextExpansionCost(state: GameState): GameNumber =
        InventoryCapacitySystem.nextExpansionCost(state.run.inventory, balanceConfig)

    override fun progressionBlocked(state: GameState): Boolean =
        InventoryCapacitySystem.isProgressionBlocked(state.run.inventory, balanceConfig)
}
