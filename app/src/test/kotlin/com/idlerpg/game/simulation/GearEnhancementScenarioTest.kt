package com.idlerpg.game.simulation

import com.idlerpg.game.core.id.ContentId
import com.idlerpg.game.core.id.InstanceId
import com.idlerpg.game.core.number.GameNumber
import com.idlerpg.game.data.content.DefaultGameContent
import com.idlerpg.game.data.local.SaveData
import com.idlerpg.game.domain.command.EnhanceItem
import com.idlerpg.game.domain.command.RefineItem
import com.idlerpg.game.domain.definition.CurrencyId
import com.idlerpg.game.domain.definition.Rarity
import com.idlerpg.game.domain.event.GearEnhancementAttempted
import com.idlerpg.game.domain.event.GearRefined
import com.idlerpg.game.domain.model.economy.CurrencyWallet
import com.idlerpg.game.domain.model.inventory.EnhancementLevel
import com.idlerpg.game.domain.model.inventory.ItemInstance
import com.idlerpg.game.domain.model.inventory.RolledAffix
import com.idlerpg.game.domain.system.inventory.EquipmentScalingSystem
import com.idlerpg.game.domain.system.inventory.GearEnhancementSystem

/** Enhancement and rolled-line refinement remain separate deterministic operations. */
object GearEnhancementScenarioTest {
    fun run() {
        val factory = SimulationTestSupport.factory()
        val runtime = com.idlerpg.game.application.GameRuntime(
            initialSession = factory.newGame(9_101L),
            sessionFactory = factory
        )
        val itemId = InstanceId(91_001L)
        val mainStat = RolledAffix(ContentId("affix.brutal"), 3L)
        val item = ItemInstance(
            instanceId = itemId,
            definitionId = DefaultGameContent.TRAINING_BLADE_ITEM_ID,
            rarity = Rarity.RARE,
            affixes = listOf(RolledAffix(DefaultGameContent.KEEN_AFFIX_ID, 4L)),
            mainStat = mainStat
        )
        val initial = runtime.state()
        runtime.replaceLoadedState(initial.copy(
            run = initial.run.copy(
                inventory = initial.run.inventory.copy(itemsById = mapOf(itemId to item)),
                economy = initial.run.economy.copy(
                    wallet = CurrencyWallet(
                        amountsByCurrencyId = mapOf(
                            CurrencyId.ENHANCEMENT_MATERIAL to GameNumber.of(100L),
                            CurrencyId.REFINEMENT_MATERIAL to GameNumber.of(10L),
                            CurrencyId.GEMS to GameNumber.of(10L)
                        )
                    )
                )
            )
        ))

        val preview = GearEnhancementSystem.preview(item)
        check(preview.targetLevel == 1)
        check(preview.successChance == GearEnhancementSystem.enhancementSuccessChance(0))
        check(
            GearEnhancementSystem.preview(item.copy(enhancementFailstack = 1)).successChance >
                preview.successChance
        )
        check(preview.currentFailstack == 0)
        check(preview.failureFailstack == 1)
        check(preview.materialCost == GameNumber.of(5L))
        check(preview.failureLevelWithoutProtection == EnhancementLevel.INITIAL)

        val first = runtime.dispatch(EnhanceItem(itemId))
        SimulationTestSupport.checkAccepted(first)
        val afterFirst = runtime.state().run.inventory.item(itemId) ?: error("missing enhanced item")
        check(afterFirst.enhancementLevel in EnhancementLevel.INITIAL..1)
        check(first.events.map { it.event }.filterIsInstance<GearEnhancementAttempted>().single().materialCost == GameNumber.of(5L))

        val highRiskItem = afterFirst.copy(enhancementLevel = EnhancementLevel.PRI)
        runtime.replaceLoadedState(runtime.state().copy(
            run = runtime.state().run.copy(
                inventory = runtime.state().run.inventory.copy(
                    itemsById = mapOf(itemId to highRiskItem)
                )
            )
        ))
        val protected = runtime.dispatch(EnhanceItem(itemId, useProtection = true))
        SimulationTestSupport.checkAccepted(protected)
        val afterProtected = runtime.state().run.inventory.item(itemId) ?: error("missing protected item")
        check(afterProtected.enhancementLevel in EnhancementLevel.PRI..EnhancementLevel.DUO)
        check(protected.events.map { it.event }.filterIsInstance<GearEnhancementAttempted>()
            .single().protectionUsed)
        check(runtime.state().run.economy.wallet.amountsByCurrencyId[CurrencyId.GEMS] == GameNumber.of(9L))

        val refined = runtime.dispatch(
            RefineItem(itemInstanceId = itemId, affixId = mainStat.affixId)
        )
        SimulationTestSupport.checkAccepted(refined)
        val afterRefine = runtime.state().run.inventory.item(itemId) ?: error("missing refined item")
        check(afterRefine.enhancementLevel == afterProtected.enhancementLevel)
        check(afterRefine.mainStat?.affixId == mainStat.affixId)
        check(refined.events.map { it.event }.filterIsInstance<GearRefined>().single().isMainStat)

        val base = EquipmentScalingSystem.scaleFlat(
            GameNumber.of(100L),
            Rarity.LEGENDARY,
            EnhancementLevel.INITIAL
        )
        val enhanced = EquipmentScalingSystem.scaleFlat(
            GameNumber.of(100L),
            Rarity.LEGENDARY,
            EnhancementLevel.PEN
        )
        check(enhanced > base)

        val persisted = afterRefine.copy(enhancementFailstack = 7)
        val persistedState = runtime.state().copy(
            run = runtime.state().run.copy(
                inventory = runtime.state().run.inventory.copy(
                    itemsById = mapOf(itemId to persisted)
                )
            )
        )
        check(SaveData.fromGameState(persistedState).toGameState().run.inventory.item(itemId)
            ?.enhancementFailstack == 7)
    }
}
