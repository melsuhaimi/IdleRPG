package com.idlerpg.game.simulation

import com.idlerpg.game.core.id.InstanceId
import com.idlerpg.game.core.config.BalanceConfig
import com.idlerpg.game.core.number.GameNumber
import com.idlerpg.game.data.content.TrainingHollowLootContent
import com.idlerpg.game.data.content.DefaultGameContent
import com.idlerpg.game.data.content.TrainingHollowQuestContent
import com.idlerpg.game.data.local.SaveData
import com.idlerpg.game.domain.command.CommandRejectionCode
import com.idlerpg.game.domain.command.ExpandInventoryCapacity
import com.idlerpg.game.domain.command.SelectRegion
import com.idlerpg.game.domain.command.StartEncounter
import com.idlerpg.game.domain.definition.CurrencyId
import com.idlerpg.game.domain.definition.Rarity
import com.idlerpg.game.domain.model.economy.CurrencyWallet
import com.idlerpg.game.domain.event.ItemSentToOverflow
import com.idlerpg.game.domain.system.reward.LootTableReward
import com.idlerpg.game.domain.system.reward.RewardBundle
import com.idlerpg.game.domain.system.reward.RewardSystem
import com.idlerpg.game.domain.model.inventory.ItemInstance
import com.idlerpg.game.domain.system.inventory.InventoryCapacitySystem
import com.idlerpg.game.domain.system.quest.QuestSystem

/** Gate 5 supersedes progression-blocking overflow: unattended combat must continue safely. */
object InventoryCapacityScenarioTest {
    fun run() {
        val runtime = SimulationTestSupport.runtime(seed = 804L)
        val base = runtime.state()
        val normal = (1L..base.run.inventory.slotCapacity).associate { value ->
            val id = InstanceId(20_000L + value)
            id to ItemInstance(id, DefaultGameContent.TRAINING_BLADE_ITEM_ID, Rarity.RARE)
        }
        val overflow = (1L..25L).associate { value ->
            val id = InstanceId(30_000L + value)
            id to ItemInstance(id, DefaultGameContent.TRAINING_BLADE_ITEM_ID, Rarity.LEGENDARY)
        }
        val full = base.copy(
            run = base.run.copy(
                inventory = base.run.inventory.copy(
                    itemsById = normal,
                    overflowItemsById = overflow
                )
            )
        )
        check(!InventoryCapacitySystem.isProgressionBlocked(full.run.inventory, SimulationTestSupport.factory().gameConfig.balance))
        check(SaveData.fromGameState(full).toGameState() == full)

        val rewardContext = SimulationTestSupport.factory().createEngineContext().also {
            it.beginExecution(full.engine)
        }
        val overflowReward = RewardSystem.grant(
            state = full,
            bundle = RewardBundle(
                lootTables = listOf(
                    LootTableReward(TrainingHollowLootContent.BOSS_LOOT_TABLE_ID)
                )
            ),
            context = rewardContext
        )
        check(overflowReward.state.run.inventory.overflowItemsById.size == overflow.size + 1) {
            "A guaranteed kept reward must spill into overflow even when soft capacity is full"
        }
        val overflowQuestReaction = QuestSystem.react(
            state = full,
            event = ItemSentToOverflow(
                itemInstanceId = InstanceId(90_001L),
                itemDefinitionId = DefaultGameContent.TRAINING_BLADE_ITEM_ID
            ),
            context = rewardContext
        )
        check(
            overflowQuestReaction.state.run.quests.progressFor(
                TrainingHollowQuestContent.questId("worthy_find")
            ).completionCount == GameNumber.ONE
        ) { "Acquiring a kept overflow item must advance item-acquisition quests" }

        val boundedCostConfig = BalanceConfig(
            baseInventoryExpansionGoldCost = GameNumber.ONE,
            inventoryExpansionGoldCostStep = GameNumber.ZERO
        )
        val corruptedCapacityRuntime = SimulationTestSupport.runtime(
            seed = 805L,
            balanceConfig = boundedCostConfig
        )
        val corruptedCapacityState = corruptedCapacityRuntime.state().copy(
            run = corruptedCapacityRuntime.state().run.copy(
                economy = corruptedCapacityRuntime.state().run.economy.copy(
                    wallet = CurrencyWallet(
                        amountsByCurrencyId = mapOf(CurrencyId.GOLD to GameNumber.ONE)
                    )
                ),
                inventory = corruptedCapacityRuntime.state().run.inventory.copy(
                    capacityUpgradePurchases = Long.MAX_VALUE
                )
            )
        )
        corruptedCapacityRuntime.replaceLoadedState(corruptedCapacityState)
        val overflowedPurchaseCounter = corruptedCapacityRuntime.dispatch(
            ExpandInventoryCapacity(quantity = 1L)
        )
        check(overflowedPurchaseCounter.commandResult is com.idlerpg.game.domain.engine.CommandResult.Rejected)
        check((overflowedPurchaseCounter.commandResult as com.idlerpg.game.domain.engine.CommandResult.Rejected)
            .reason.code == CommandRejectionCode.CAPACITY_EXCEEDED)

        runtime.replaceLoadedState(full)
        SimulationTestSupport.checkAccepted(runtime.dispatch(SelectRegion(DefaultGameContent.TRAINING_HOLLOW_REGION_ID)))
        SimulationTestSupport.checkAccepted(runtime.dispatch(StartEncounter(DefaultGameContent.TRAINING_SLIME_ENCOUNTER_ID)))
    }
}
