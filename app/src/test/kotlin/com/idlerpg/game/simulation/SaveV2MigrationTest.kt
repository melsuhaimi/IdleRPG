package com.idlerpg.game.simulation

import com.idlerpg.game.core.config.BalanceConfig
import com.idlerpg.game.core.number.GameNumber
import com.idlerpg.game.core.id.InstanceId
import com.idlerpg.game.core.time.GameDuration
import com.idlerpg.game.data.content.DefaultGameContent
import com.idlerpg.game.data.local.SaveCodec
import com.idlerpg.game.data.local.SaveData
import com.idlerpg.game.data.local.SaveEnvelope
import com.idlerpg.game.data.local.SaveVersion
import com.idlerpg.game.data.local.migration.SaveMigrationRegistry
import com.idlerpg.game.domain.definition.CurrencyId
import com.idlerpg.game.domain.definition.Rarity
import com.idlerpg.game.domain.model.inventory.ItemInstance
import com.idlerpg.game.domain.model.achievement.AchievementProgressState
import com.idlerpg.game.domain.model.chronicle.EchoState
import com.idlerpg.game.domain.system.economy.TransactionSystem

/**
 * FBE-00 regression for exact V1 -> V2 migration and V2 deterministic round-trip.
 *
 * The fixture intentionally creates more than the new base inventory capacity so the
 * migration must grandfather every owned item by raising slotCapacity instead of losing
 * or moving any item.
 */
object SaveV2MigrationTest {

    fun run() {
        val runtime = SimulationTestSupport.runtime(
            seed = 5150L,
            balanceConfig = BalanceConfig(
                baseInventoryCapacity = 100L,
                maximumInventoryCapacity = 300L
            )
        )
        SimulationTestSupport.startTraining(runtime)
        runtime.advance(GameDuration.ofSeconds(60L))
        val generated = (1L..65L).associate { value ->
            val id = InstanceId(70_000L + value)
            id to ItemInstance(id, DefaultGameContent.TRAINING_BLADE_ITEM_ID, Rarity.UNCOMMON)
        }
        val base = runtime.state().copy(
            run = runtime.state().run.copy(
                inventory = runtime.state().run.inventory.copy(itemsById = generated)
            )
        )
        check(base.run.inventory.itemsById.size > BalanceConfig.DEFAULT_BASE_INVENTORY_CAPACITY)

        val questProgress =
            base.run.quests.progressFor(DefaultGameContent.FIRST_HUNT_QUEST_ID)
        check(questProgress.completionCount == GameNumber.ONE)
        check(questProgress.claimedCount == GameNumber.ZERO)
        val legacyQuestProgress = questProgress.copy(claimedCount = questProgress.completionCount)
        val legacyEconomy = TransactionSystem.grant(
            economy = base.run.economy,
            currencyId = CurrencyId.GOLD,
            amount = GameNumber.of(25L)
        )

        val achievementProgress = AchievementProgressState(
            progressByObjectiveId = mapOf(
                DefaultGameContent.FORGED_FLAME_ACHIEVEMENT_OBJECTIVE_ID to GameNumber.ONE
            ),
            completed = true,
            rewardClaimed = true
        )

        val expectedCapacity = base.run.inventory.itemsById.size.toLong()
        val expected = base.copy(
            run = base.run.copy(
                economy = legacyEconomy,
                quests = base.run.quests.copy(
                    progressByQuestId = base.run.quests.progressByQuestId +
                        (DefaultGameContent.FIRST_HUNT_QUEST_ID to legacyQuestProgress)
                ),
                inventory = base.run.inventory.copy(
                    slotCapacity = expectedCapacity,
                    capacityUpgradePurchases = 0L,
                    overflowItemsById = emptyMap()
                )
            ),
            meta = base.meta.copy(
                achievements = base.meta.achievements.copy(
                    progressByAchievementId =
                        base.meta.achievements.progressByAchievementId +
                            (DefaultGameContent.FORGED_FLAME_ACHIEVEMENT_ID to
                                achievementProgress)
                ),
                echoes = EchoState(
                    available = GameNumber.of(2L),
                    spent = GameNumber.of(3L),
                    purchasedOfferIds = setOf(
                        DefaultGameContent.ADAPTATION_FORECAST_ECHO_UNLOCK_ID
                    )
                ),
                discoveries = base.meta.discoveries.copy(
                    unlockedHiddenContentIds =
                        base.meta.discoveries.unlockedHiddenContentIds +
                            DefaultGameContent.ADAPTATION_FORECAST_DISCOVERY_ID
                )
            )
        )

        val v2Data = SaveData.fromGameState(expected)
        val v1Fields = v2Data.fields.filterKeys { key ->
            !isV2OnlyField(key)
        }

        val v1Envelope = SaveEnvelope(
            schemaVersion = SaveVersion.V1,
            contentVersion = SimulationTestSupport.CONTENT_VERSION,
            writtenAtEpochMs = 777_000L,
            data = SaveData(v1Fields)
        )

        val migrated = SaveMigrationRegistry().migrate(
            envelope = v1Envelope,
            targetVersion = SaveVersion.V3
        )

        check(migrated.schemaVersion == SaveVersion.V3)
        val migratedState = migrated.gameState()
        check(migratedState == expected)
        check(migratedState.run.inventory.itemsById.size == base.run.inventory.itemsById.size)
        check(migratedState.run.inventory.overflowItemsById.isEmpty())
        check(
            migratedState.run.inventory.slotCapacity >=
                migratedState.run.inventory.itemsById.size.toLong()
        )

        val codec = SaveCodec()
        val encoded = codec.encode(migrated)
        val decoded = codec.decode(encoded)
        check(decoded.schemaVersion == SaveVersion.V3)
        check(decoded.gameState() == expected)
        check(encoded.contentEquals(codec.encode(migrated)))
    }

    private fun isV2OnlyField(key: String): Boolean =
        key.startsWith("run.combat.queuedPlayerAction.") ||
            key.endsWith(".claimedCount") ||
            key.endsWith(".rewardClaimed") ||
            key.startsWith("meta.echoes.purchasedOfferIds.") ||
            key == "run.inventory.slotCapacity" ||
            key == "run.inventory.capacityUpgradePurchases" ||
            key.startsWith("run.inventory.overflowItemsById.") ||
            key == "run.world.currentEncounter.currentWave"
}
