package com.idlerpg.game.domain.system.loot

import com.idlerpg.game.core.id.ContentId
import com.idlerpg.game.data.content.ContentRegistry
import com.idlerpg.game.domain.engine.EngineContext
import com.idlerpg.game.domain.event.AffixRolled
import com.idlerpg.game.domain.event.GameEvent
import com.idlerpg.game.domain.event.ItemAdded
import com.idlerpg.game.domain.event.ItemDropped
import com.idlerpg.game.domain.event.ItemSentToOverflow
import com.idlerpg.game.domain.event.ItemAutoSalvaged
import com.idlerpg.game.domain.event.CurrencyGranted
import com.idlerpg.game.domain.definition.CurrencyId
import com.idlerpg.game.domain.definition.Rarity
import com.idlerpg.game.domain.definition.item.EquipmentDefinition
import com.idlerpg.game.domain.model.GameState
import com.idlerpg.game.domain.model.inventory.ItemInstance
import com.idlerpg.game.domain.model.inventory.LootFilterState
import com.idlerpg.game.domain.system.inventory.GeneratedItemPlacement
import com.idlerpg.game.domain.system.inventory.InventorySystem
import com.idlerpg.game.domain.system.economy.TransactionSystem

data class LootGrantResult(
    val state: GameState,
    val events: List<GameEvent>
)

/** Deterministic loot roll plus bounded normal/overflow ownership transfer. */
object LootSystem {

    /**
     * Rarity filters may discard ordinary stat sticks, but never an authored item whose
     * bounded effects can change a skill, Resonance sequence, or affinity plan.
     */
    fun shouldKeepGeneratedItem(
        filter: LootFilterState,
        equipmentDefinition: EquipmentDefinition?,
        rarity: Rarity
    ): Boolean = filter.shouldKeep(rarity) ||
        equipmentDefinition?.hasActiveBuildDefiningTrait(rarity) == true

    fun requiredItemSlots(
        lootTableId: ContentId,
        contentRegistry: ContentRegistry
    ): Long = contentRegistry.lootTable(lootTableId).rolls.toLong()

    fun rollAndGrant(
        state: GameState,
        lootTableId: ContentId,
        sourceDefinitionId: ContentId?,
        context: EngineContext
    ): LootGrantResult {
        val table = context.contentRegistry.lootTable(lootTableId)
        val selections = LootTableSystem.roll(
            definition = table,
            contentRegistry = context.contentRegistry,
            random = context.random
        )
        var inventory = state.run.inventory
        var economy = state.run.economy
        val events = mutableListOf<GameEvent>()

        for (selection in selections) {
            val itemDefinition = context.contentRegistry.item(selection.itemDefinitionId)
            val instanceId = context.nextInstanceId()
            val affixes = AffixRollSystem.roll(
                itemDefinition,
                selection.rarity,
                context.contentRegistry,
                context.random
            )
            val item = ItemInstance(
                instanceId = instanceId,
                definitionId = itemDefinition.id,
                rarity = selection.rarity,
                affixes = affixes,
                sourceDefinitionId = sourceDefinitionId
            )
            events += ItemDropped(item.instanceId, item.definitionId, item.rarity, sourceDefinitionId)
            item.affixes.sortedBy { it.affixId }.forEach { rolled ->
                events += AffixRolled(item.instanceId, rolled.affixId, rolled.value)
            }
            if (!shouldKeepGeneratedItem(
                    inventory.lootFilter,
                    itemDefinition.equipmentDefinitionId
                        ?.let(context.contentRegistry::equipmentOrNull),
                    item.rarity
                )
            ) {
                val gold = itemDefinition.salvageProfile.goldFor(item.rarity)
                if (gold > com.idlerpg.game.core.number.GameNumber.ZERO) {
                    economy = TransactionSystem.grant(economy, CurrencyId.GOLD, gold)
                    events += CurrencyGranted(CurrencyId.GOLD, gold, item.definitionId)
                }
                events += ItemAutoSalvaged(item.instanceId, item.definitionId, item.rarity, gold)
            } else {
                val add = InventorySystem.addGeneratedItem(inventory, item, context.balanceConfig)
                inventory = add.inventory
                events += when (add.placement) {
                    GeneratedItemPlacement.NORMAL -> ItemAdded(item.instanceId, item.definitionId)
                    GeneratedItemPlacement.OVERFLOW ->
                        ItemSentToOverflow(item.instanceId, item.definitionId)
                }
            }
        }

        return LootGrantResult(
            state.copy(run = state.run.copy(inventory = inventory, economy = economy)),
            events
        )
    }
}
