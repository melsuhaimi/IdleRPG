package com.idlerpg.game.presentation

import com.idlerpg.game.core.config.BalanceConfig
import com.idlerpg.game.core.id.InstanceId
import com.idlerpg.game.core.number.GameNumber
import com.idlerpg.game.data.content.ContentRegistry
import com.idlerpg.game.data.content.DefaultGameContent
import com.idlerpg.game.domain.definition.CurrencyId
import com.idlerpg.game.domain.definition.EquipmentSlot
import com.idlerpg.game.domain.definition.Rarity
import com.idlerpg.game.domain.model.GameState
import com.idlerpg.game.domain.model.inventory.EquipmentLoadoutState
import com.idlerpg.game.domain.model.inventory.InventoryState
import com.idlerpg.game.domain.model.inventory.ItemInstance
import com.idlerpg.game.domain.model.inventory.ItemLockState
import com.idlerpg.game.domain.model.inventory.RolledAffix
import com.idlerpg.game.domain.model.economy.CurrencyWallet
import com.idlerpg.game.presentation.content.PresentationContentRegistry
import com.idlerpg.game.presentation.model.GearEffectKind
import com.idlerpg.game.presentation.projection.GearProjector
import com.idlerpg.game.presentation.query.DefaultGearReadQueries
import org.junit.Test

object GearProjectionTest {
    fun run() {
        val contentRegistry = ContentRegistry(DefaultGameContent.create())
        val presentationRegistry = PresentationContentRegistry.default()
        val balance = BalanceConfig(
            baseInventoryCapacity = 2L,
            inventoryCapacityPerExpansion = 2L,
            maximumInventoryCapacity = 6L,
            baseInventoryExpansionGoldCost = GameNumber.of(20L),
            inventoryExpansionGoldCostStep = GameNumber.of(10L),
            inventoryOverflowCapacity = 2L
        )
        val projector = GearProjector(
            contentRegistry = contentRegistry,
            presentationContentRegistry = presentationRegistry,
            readQueries = DefaultGearReadQueries(balance)
        )

        val bladeId = InstanceId(10L)
        val catalystId = InstanceId(11L)
        val overflowId = InstanceId(12L)
        val candidateBladeId = InstanceId(13L)
        val blade = ItemInstance(
            instanceId = bladeId,
            definitionId = DefaultGameContent.TRAINING_BLADE_ITEM_ID,
            rarity = Rarity.UNCOMMON,
            affixes = listOf(RolledAffix(DefaultGameContent.KEEN_AFFIX_ID, 3L))
        )
        val catalyst = ItemInstance(
            instanceId = catalystId,
            definitionId = DefaultGameContent.TRAINING_CATALYST_ITEM_ID,
            rarity = Rarity.COMMON
        )
        val candidateBlade = ItemInstance(
            instanceId = candidateBladeId,
            definitionId = DefaultGameContent.TRAINING_BLADE_ITEM_ID,
            rarity = Rarity.COMMON
        )
        val overflowBlade = ItemInstance(
            instanceId = overflowId,
            definitionId = DefaultGameContent.TRAINING_BLADE_ITEM_ID,
            rarity = Rarity.RARE
        )

        val state = GameState.newGame(707L).copy(
            run = GameState.newGame(707L).run.copy(
                inventory = InventoryState(
                    itemsById = sortedMapOf(
                        bladeId to blade,
                        catalystId to catalyst,
                        candidateBladeId to candidateBlade
                    ),
                    equipment = EquipmentLoadoutState(
                        mapOf(EquipmentSlot.WEAPON to bladeId)
                    ),
                    locks = ItemLockState(setOf(catalystId)),
                    slotCapacity = 3L,
                    overflowItemsById = sortedMapOf(overflowId to overflowBlade)
                ),
                economy = GameState.newGame(707L).run.economy.copy(
                    wallet = CurrencyWallet(
                        mapOf(CurrencyId.GOLD to GameNumber.of(50L))
                    )
                )
            )
        )

        val ui = projector.project(state)
        check(ui.capacity.normalUsed == 3L)
        check(ui.capacity.normalCapacity == 3L)
        check(ui.capacity.normalAvailable == 0L)
        check(ui.capacity.overflowUsed == 1L)
        check(ui.capacity.overflowCapacity == 2L)
        check(ui.capacity.nextExpansionGoldCostDisplay == "20")
        check(ui.capacity.canAffordExpansion)
        check(!ui.capacity.progressionBlocked)

        val weapon = ui.equipmentSlots.single { it.slot == EquipmentSlot.WEAPON }
        check(weapon.equippedItem?.instanceId == bladeId)
        val projectedBlade = ui.ownedItems.single { it.instanceId == bladeId }
        check(projectedBlade.iconAssetKey ==
            com.idlerpg.game.presentation.content.PresentationAssetKey.TRAINING_BLADE)
        check(projectedBlade.equippedSlot == EquipmentSlot.WEAPON)
        check(!projectedBlade.canSalvage)
        check(projectedBlade.affixes.single().rolledValue == 3L)
        check(projectedBlade.affixes.single().iconAssetKey ==
            com.idlerpg.game.presentation.content.PresentationAssetKey.KEEN)
        check(projectedBlade.effects.single().kind == GearEffectKind.FLAT_ATTACK_POWER)

        val projectedCatalyst = ui.ownedItems.single { it.instanceId == catalystId }
        check(projectedCatalyst.locked)
        check(!projectedCatalyst.canSalvage)
        check(projectedCatalyst.canUnlock)
        check(projectedCatalyst.effects.single().kind == GearEffectKind.RESONANCE_CHARGE_BONUS)
        check(projectedCatalyst.comparison != null)
        check(projectedCatalyst.comparison!!.currentAttackDisplay.isNotBlank())
        check(projectedCatalyst.comparison!!.resultingArmorDisplay.isNotBlank())

        val projectedCandidate = ui.ownedItems.single { it.instanceId == candidateBladeId }
        check(projectedCandidate.comparison != null)
        check(projectedCandidate.comparison!!.currentAttackDisplay !=
            projectedCandidate.comparison!!.resultingAttackDisplay)
        check(projectedCandidate.comparison!!.attackDeltaDisplay != "0")

        val projectedOverflow = ui.overflowItems.single()
        check(projectedOverflow.instanceId == overflowId)
        check(!projectedOverflow.canClaimOverflow)
        check(projectedOverflow.canSalvageOverflow)

        val blockedState = state.copy(
            run = state.run.copy(
                inventory = state.run.inventory.copy(
                    overflowItemsById = sortedMapOf(
                        overflowId to overflowBlade,
                        InstanceId(14L) to overflowBlade.copy(instanceId = InstanceId(14L))
                    )
                )
            )
        )
        val blocked = projector.project(blockedState)
        check(blocked.capacity.progressionBlocked)

        println("FUI07_GEAR_PROJECTION_PASS")
    }
}

/** Conventional Gradle/JUnit entry point for the projection contract above. */
class GearProjectionJUnitTest {
    @Test
    fun gearProjectionIncludesAuthoritativeComparison() {
        GearProjectionTest.run()
    }
}
