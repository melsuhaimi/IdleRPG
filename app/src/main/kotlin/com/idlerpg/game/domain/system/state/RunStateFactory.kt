package com.idlerpg.game.domain.system.state

import com.idlerpg.game.core.config.BalanceConfig
import com.idlerpg.game.domain.model.RunState
import com.idlerpg.game.domain.model.MetaState
import com.idlerpg.game.domain.model.economy.EconomyState
import com.idlerpg.game.domain.model.economy.CurrencyWallet
import com.idlerpg.game.domain.definition.CurrencyId
import com.idlerpg.game.core.number.GameNumber
import com.idlerpg.game.data.content.DefaultGameContent
import com.idlerpg.game.domain.model.inventory.InventoryState
import com.idlerpg.game.domain.model.world.RegionProgressState
import com.idlerpg.game.domain.model.world.WorldState

/**
 * Single configured owner for constructing a fresh Chronicle-resettable [RunState].
 *
 * New-game creation and Chronicle collapse must use the same factory so configuration-
 * owned defaults such as inventory capacity cannot diverge between those two paths.
 */
object RunStateFactory {

    fun fresh(balanceConfig: BalanceConfig): RunState =
        fresh(balanceConfig, MetaState())
    fun fresh(balanceConfig: BalanceConfig, meta: MetaState): RunState {
        val expeditionMemory = DefaultGameContent.EXPEDITION_MEMORY_FEATURE_ID in
            meta.persistentFeatureUnlocks.unlockedFeatureIds
        val compressedEncounters = if (expeditionMemory) {
            listOf(
                DefaultGameContent.TRAINING_SLIME_ENCOUNTER_ID,
                DefaultGameContent.RIFTFANG_ENCOUNTER_ID,
                DefaultGameContent.CINDER_WISP_ENCOUNTER_ID
            )
        } else emptyList()
        return RunState(
            economy = EconomyState(
                wallet = CurrencyWallet(amountsByCurrencyId = if (DefaultGameContent.LEGACY_ACCELERATION_FEATURE_ID in meta.persistentFeatureUnlocks.unlockedFeatureIds) mapOf(CurrencyId.GOLD to GameNumber.of(100L)) else emptyMap()),
                upgrades = com.idlerpg.game.domain.model.economy.UpgradeProgressState(
                    com.idlerpg.game.domain.system.chronicle.EchoTrainingSystem.startingLevels(meta)
                )
            ),
            inventory = InventoryState(
                slotCapacity = balanceConfig.baseInventoryCapacity
            ),
            world = if (expeditionMemory) {
                WorldState(
                    activeRegionId = DefaultGameContent.TRAINING_HOLLOW_REGION_ID,
                    unlockedRegionIds = setOf(DefaultGameContent.TRAINING_HOLLOW_REGION_ID),
                    regionProgressById = mapOf(
                        DefaultGameContent.TRAINING_HOLLOW_REGION_ID to RegionProgressState(
                            highestClearedEncounterTier = 3L,
                            normalClears = GameNumber.of(3L)
                        )
                    ),
                    selectedFarmEncounterId = compressedEncounters.last(),
                    clearedEncounterIds = compressedEncounters.toSet()
                )
            } else WorldState()
        )
    }
}
