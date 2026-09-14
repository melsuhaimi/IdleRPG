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
        check(ui.capacity.normalUsed == 3L) { "normal inventory count changed" }
        check(ui.capacity.normalCapacity == 3L) { "normal inventory capacity changed" }
        check(ui.capacity.normalAvailable == 0L) { "normal inventory availability changed" }
        check(ui.capacity.overflowUsed == 1L) { "overflow inventory count changed" }
        check(ui.capacity.overflowCapacity == 2L) { "overflow capacity changed" }
        check(ui.capacity.nextExpansionGoldCostDisplay == "20") { "expansion cost changed" }
        check(ui.capacity.canAffordExpansion) { "expansion affordability changed" }
        check(!ui.capacity.progressionBlocked) { "fresh overflow state is blocked" }

        val weapon = ui.equipmentSlots.single { it.slot == EquipmentSlot.WEAPON }
        check(weapon.equippedItem?.instanceId == bladeId) { "weapon projection lost its equipped blade" }
        val projectedBlade = ui.ownedItems.single { it.instanceId == bladeId }
        check(projectedBlade.iconAssetKey ==
            com.idlerpg.game.presentation.content.PresentationAssetKey.TRAINING_BLADE)
        check(projectedBlade.equippedSlot == EquipmentSlot.WEAPON) { "blade slot projection changed" }
        check(!projectedBlade.canSalvage) { "equipped blade became salvageable" }
        check(projectedBlade.affixes.single().rolledValue == 3L) { "blade affix roll changed" }
        check(projectedBlade.affixes.single().iconAssetKey ==
            com.idlerpg.game.presentation.content.PresentationAssetKey.KEEN)
        check(projectedBlade.effects.any { it.kind == GearEffectKind.FLAT_ATTACK_POWER }) { "blade lost its baseline attack effect" }

        val projectedCatalyst = ui.ownedItems.single { it.instanceId == catalystId }
        check(projectedCatalyst.locked) { "locked catalyst projection changed" }
        check(!projectedCatalyst.canSalvage) { "locked catalyst became salvageable" }
        check(projectedCatalyst.canUnlock) { "locked catalyst cannot be unlocked" }
        check(projectedCatalyst.effects.any { it.kind == GearEffectKind.RESONANCE_CHARGE_BONUS }) { "common catalyst lost its baseline resonance effect" }
        check(projectedCatalyst.comparison != null) { "catalyst comparison is missing" }
        check(projectedCatalyst.comparison!!.currentAttackDisplay.isNotBlank()) { "catalyst attack comparison is blank" }
        check(projectedCatalyst.comparison!!.resultingArmorDisplay.isNotBlank()) { "catalyst armor comparison is blank" }

        val projectedCandidate = ui.ownedItems.single { it.instanceId == candidateBladeId }
        check(projectedCandidate.comparison != null) { "candidate blade comparison is missing" }
        check(projectedCandidate.comparison!!.currentAttackDisplay !=
            projectedCandidate.comparison!!.resultingAttackDisplay)
        check(projectedCandidate.comparison!!.attackDeltaDisplay != "0") { "candidate blade has no attack delta" }

        val projectedOverflow = ui.overflowItems.single()
        check(projectedOverflow.instanceId == overflowId) { "overflow item identity changed" }
        check(!projectedOverflow.canClaimOverflow) { "full inventory can claim overflow" }
        check(projectedOverflow.canSalvageOverflow) { "overflow item cannot be salvaged" }

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
        check(!blocked.capacity.progressionBlocked) { "overflow storage incorrectly blocked unattended progression" }

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
