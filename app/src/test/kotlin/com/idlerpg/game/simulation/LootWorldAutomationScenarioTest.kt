package com.idlerpg.game.simulation

import com.idlerpg.game.core.id.InstanceId
import com.idlerpg.game.core.random.SeededGameRandom
import com.idlerpg.game.data.content.DefaultGameContent
import com.idlerpg.game.data.local.SaveData
import com.idlerpg.game.data.local.SaveEnvelope
import com.idlerpg.game.data.local.SaveVersion
import com.idlerpg.game.data.local.migration.SaveMigrationRegistry
import com.idlerpg.game.domain.command.ConfigureWorldAutomation
import com.idlerpg.game.domain.command.SalvageItems
import com.idlerpg.game.domain.command.SalvageOverflowItems
import com.idlerpg.game.domain.command.SelectRegion
import com.idlerpg.game.domain.definition.EquipmentSlot
import com.idlerpg.game.domain.definition.Rarity
import com.idlerpg.game.domain.engine.CommandResult
import com.idlerpg.game.domain.model.inventory.ItemInstance
import com.idlerpg.game.domain.model.inventory.ItemLockState
import com.idlerpg.game.domain.model.inventory.EquipmentLoadoutState
import com.idlerpg.game.domain.model.world.RegionProgressState
import com.idlerpg.game.domain.system.stats.ModifierSystem
import com.idlerpg.game.domain.system.world.RegionSystem
import com.idlerpg.game.core.number.GameNumber
import com.idlerpg.game.domain.definition.Affinity
import com.idlerpg.game.data.content.TrainingHollowLootContent
import com.idlerpg.game.data.content.TrainingHollowStrategyContent
import com.idlerpg.game.data.content.TrainingHollowWorldContent
import com.idlerpg.game.domain.model.world.WorldAutomationMode
import com.idlerpg.game.domain.model.world.EncounterState
import com.idlerpg.game.domain.model.world.EncounterStatus
import com.idlerpg.game.domain.system.loot.LootTableSystem
import com.idlerpg.game.domain.system.loot.LootSystem
import com.idlerpg.game.domain.model.inventory.LootFilterState

/** Gate 5–6 acceptance: sane loot, atomic bulk QoL, 30 stages, and Push/Farm persistence. */
object LootWorldAutomationScenarioTest {
    fun run() {
        val factory = SimulationTestSupport.factory()
        val registry = factory.contentRegistry
        check(EquipmentSlot.values().size == 6)
        check(registry.allItems().size == 11)
        check(registry.allAffixes().size == 16)
        val region = registry.region(DefaultGameContent.TRAINING_HOLLOW_REGION_ID)
        check(region.encounterIds.size == TrainingHollowWorldContent.MAX_STAGE)
        check(region.encounterIds.toSet().size == TrainingHollowWorldContent.MAX_STAGE)
        region.encounterIds.zipWithNext().forEach { (current, next) ->
            check(registry.encounter(current).nextEncounterId == next)
        }

        val random = SeededGameRandom(5_600L)
        val table = registry.lootTable(DefaultGameContent.TRAINING_SLIME_LOOT_TABLE_ID)
        val drops = (1..10_000).sumOf {
            LootTableSystem.roll(table, registry, random).size
        }
        check(drops in 500..900) { "Expected low single-digit loot rate, got $drops/10000" }
        val eliteDrops = (1..10_000).sumOf {
            LootTableSystem.roll(registry.lootTable(TrainingHollowLootContent.ELITE_LOOT_TABLE_ID), registry, random).size
        }
        check(eliteDrops > drops * 2)

        val selectableKeepTiers = Rarity.ordered()
        selectableKeepTiers.forEach { keepTier ->
            val filter = LootFilterState(
                autoSalvageEnabled = true,
                minimumKeepRarity = keepTier
            )
            Rarity.ordered().forEach { rarity ->
                check(filter.shouldKeep(rarity) == (rarity.rank >= keepTier.rank)) {
                    "Auto-salvage threshold $keepTier must keep only $keepTier and above; got $rarity"
                }
            }
        }

        val strictFilter = LootFilterState(autoSalvageEnabled = true, minimumKeepRarity = Rarity.RARE)
        check(
            !LootSystem.shouldKeepGeneratedItem(
                strictFilter,
                registry.equipment(DefaultGameContent.TRAINING_BLADE_EQUIPMENT_ID),
                Rarity.COMMON
            )
        ) { "Inactive low-rarity traits must not defeat the junk filter" }
        check(
            LootSystem.shouldKeepGeneratedItem(
                strictFilter,
                registry.equipment(DefaultGameContent.TRAINING_BLADE_EQUIPMENT_ID),
                Rarity.RARE
            )
        ) { "Active build-defining traits must survive auto-salvage" }

        val runtime = SimulationTestSupport.runtime(seed = 5_601L)
        val emptyOverflowFailure = runCatching {
            runtime.dispatch(SalvageOverflowItems(emptySet()))
        }.exceptionOrNull()
        check(emptyOverflowFailure is IllegalArgumentException)
        val first = InstanceId(51_001L)
        val second = InstanceId(51_002L)
        val items = mapOf(
            first to ItemInstance(first, DefaultGameContent.TRAINING_BLADE_ITEM_ID, Rarity.COMMON),
            second to ItemInstance(second, DefaultGameContent.TRAINING_BLADE_ITEM_ID, Rarity.UNCOMMON)
        )
        runtime.replaceLoadedState(
            runtime.state().copy(
                run = runtime.state().run.copy(
                    inventory = runtime.state().run.inventory.copy(
                        itemsById = items,
                        locks = ItemLockState(setOf(second))
                    )
                )
            )
        )
        check(runtime.dispatch(SalvageItems(setOf(first, second))).commandResult is CommandResult.Rejected)
        check(runtime.state().run.inventory.itemsById.size == 2)
        runtime.replaceLoadedState(
            runtime.state().copy(
                run = runtime.state().run.copy(
                    inventory = runtime.state().run.inventory.copy(locks = ItemLockState())
                )
            )
        )
        SimulationTestSupport.checkAccepted(runtime.dispatch(SalvageItems(setOf(first, second))))
        check(runtime.state().run.inventory.itemsById.isEmpty())

        val traitRuntime = SimulationTestSupport.runtime(seed = 5_603L)
        val traitDefinitions = listOf(
            DefaultGameContent.TRAINING_BLADE_ITEM_ID,
            DefaultGameContent.TRAINING_CATALYST_ITEM_ID,
            TrainingHollowLootContent.ARMOR_ITEM_ID,
            TrainingHollowLootContent.HELM_ITEM_ID,
            TrainingHollowLootContent.BOOTS_ITEM_ID,
            TrainingHollowLootContent.ACCESSORY_ITEM_ID,
            TrainingHollowLootContent.VOIDGLASS_EDGE_ITEM_ID,
            TrainingHollowLootContent.WARDEN_PLATE_ITEM_ID,
            TrainingHollowLootContent.STARFALL_VISOR_ITEM_ID,
            TrainingHollowLootContent.RESONANT_CORE_ITEM_ID,
            TrainingHollowLootContent.DUSK_SIGIL_ITEM_ID
        )
        val traitItemIds = traitDefinitions.indices.map { index -> InstanceId(52_000L + index) }
        val traitItems = traitDefinitions.mapIndexed { index, definitionId ->
            traitItemIds[index] to ItemInstance(traitItemIds[index], definitionId, Rarity.RARE)
        }.toMap()
        val equipped = mapOf(
            EquipmentSlot.WEAPON to traitItemIds[0],
            EquipmentSlot.ARMOR to traitItemIds[2],
            EquipmentSlot.HELM to traitItemIds[3],
            EquipmentSlot.BOOTS to traitItemIds[4],
            EquipmentSlot.ACCESSORY to traitItemIds[5],
            EquipmentSlot.CATALYST to traitItemIds[1]
        )
        traitRuntime.replaceLoadedState(traitRuntime.state().copy(
            run = traitRuntime.state().run.copy(
                inventory = traitRuntime.state().run.inventory.copy(
                    itemsById = traitItems,
                    equipment = EquipmentLoadoutState(equipped)
                )
            )
        ))
        val traitState = traitRuntime.state()
        check(ModifierSystem.skillCleaveRatio(traitState, DefaultGameContent.HEAVY_STRIKE_ID, registry) != null)
        check(ModifierSystem.preservedSequenceEntries(traitState, DefaultGameContent.ARCANE_PULSE_ID, registry) == 1)
        check(ModifierSystem.skillHealingMultiplier(traitState, DefaultGameContent.GUARD_MEND_ID, registry).units > 10_000L)
        check(ModifierSystem.skillResonanceBonuses(traitState, DefaultGameContent.QUICK_SLASH_ID, registry)[Affinity.TEMPO] == GameNumber.ONE)
        val buildDefiningEquipped = equipped + mapOf(
            EquipmentSlot.WEAPON to traitItemIds[6],
            EquipmentSlot.ARMOR to traitItemIds[7],
            EquipmentSlot.HELM to traitItemIds[8],
            EquipmentSlot.CATALYST to traitItemIds[9],
            EquipmentSlot.ACCESSORY to traitItemIds[10]
        )
        val buildDefiningState = traitState.copy(
            run = traitState.run.copy(
                inventory = traitState.run.inventory.copy(
                    equipment = EquipmentLoadoutState(buildDefiningEquipped)
                )
            )
        )
        check(
            ModifierSystem.skillCleaveRatio(
                buildDefiningState,
                TrainingHollowStrategyContent.VOID_LANCE_ID,
                registry
            ) != null
        )
        check(
            ModifierSystem.skillHealingMultiplier(
                buildDefiningState,
                TrainingHollowStrategyContent.IRON_VOW_ID,
                registry
            ).units > 10_000L
        )
        check(
            ModifierSystem.preservedSequenceEntries(
                buildDefiningState,
                TrainingHollowStrategyContent.STARFALL_ID,
                registry
            ) == 1
        )

        val worldRuntime = SimulationTestSupport.runtime(seed = 5_602L)
        SimulationTestSupport.checkAccepted(
            worldRuntime.dispatch(SelectRegion(DefaultGameContent.TRAINING_HOLLOW_REGION_ID))
        )
        val cleared = DefaultGameContent.TRAINING_SLIME_ENCOUNTER_ID
        worldRuntime.replaceLoadedState(
            worldRuntime.state().copy(
                run = worldRuntime.state().run.copy(
                    world = worldRuntime.state().run.world.copy(clearedEncounterIds = setOf(cleared))
                )
            )
        )
        SimulationTestSupport.checkAccepted(
            worldRuntime.dispatch(
                ConfigureWorldAutomation(WorldAutomationMode.FARM, cleared)
            )
        )
        check(worldRuntime.state().run.world.automationMode == WorldAutomationMode.FARM)
        check(worldRuntime.state().run.world.selectedFarmEncounterId == cleared)

        val unknownFarmId = SimulationTestSupport.contentId("encounter.not_authored")
        worldRuntime.replaceLoadedState(
            worldRuntime.state().copy(
                run = worldRuntime.state().run.copy(
                    world = worldRuntime.state().run.world.copy(
                        selectedFarmEncounterId = null,
                        clearedEncounterIds = setOf(unknownFarmId)
                    )
                )
            )
        )
        val invalidPush = worldRuntime.dispatch(
            ConfigureWorldAutomation(WorldAutomationMode.PUSH, unknownFarmId)
        )
        check(invalidPush.commandResult is CommandResult.Rejected)
        check((invalidPush.commandResult as CommandResult.Rejected).reason.code ==
            com.idlerpg.game.domain.command.CommandRejectionCode.UNKNOWN_CONTENT)

        val malformedAutomationRuntime = SimulationTestSupport.runtime(seed = 5_605L)
        SimulationTestSupport.checkAccepted(
            malformedAutomationRuntime.dispatch(
                SelectRegion(DefaultGameContent.TRAINING_HOLLOW_REGION_ID)
            )
        )
        malformedAutomationRuntime.replaceLoadedState(
            malformedAutomationRuntime.state().copy(
                run = malformedAutomationRuntime.state().run.copy(
                    world = malformedAutomationRuntime.state().run.world.copy(
                        currentEncounter = EncounterState(
                            definitionId = cleared,
                            encounterIndex = 1L,
                            encounterSeed = 5_605L,
                            status = EncounterStatus.CLEARED,
                            rewardEligible = false
                        ),
                        automationMode = WorldAutomationMode.FARM,
                        selectedFarmEncounterId = unknownFarmId,
                        clearedEncounterIds = setOf(cleared, unknownFarmId)
                    )
                )
            )
        )
        val malformedRecovery = malformedAutomationRuntime.advance(com.idlerpg.game.core.time.GameDuration.ZERO)
        check(malformedRecovery.state.run.world.currentEncounter?.status == EncounterStatus.ACTIVE) {
            "A malformed persisted farm target must not crash the autonomous lifecycle"
        }
        check(malformedRecovery.state.run.world.currentEncounter?.definitionId == cleared)

        val once = RegionSystem.recordEncounterClear(
            worldRuntime.state(), registry.encounter(cleared), 1L, registry
        ).state
        val twice = RegionSystem.recordEncounterClear(
            once, registry.encounter(cleared), 1L, registry
        ).state
        check(twice.run.world.regionProgressById.getValue(DefaultGameContent.TRAINING_HOLLOW_REGION_ID)
            .highestClearedEncounterTier == 1L)

        val current = worldRuntime.state().copy(
            run = worldRuntime.state().run.copy(
                world = worldRuntime.state().run.world.copy(
                    automationMode = WorldAutomationMode.PUSH,
                    selectedFarmEncounterId = null,
                    clearedEncounterIds = emptySet()
                )
            )
        )
        val currentData = SaveData.fromGameState(current)
        val v3Fields = currentData.fields.filterKeys { key ->
            !key.startsWith("run.inventory.lootFilter.") &&
                !key.startsWith("run.player.selectedSkillEvolutionBySkillId.") &&
                !key.startsWith("meta.heroName.") &&
                !key.startsWith("meta.rebirth.") &&
                !key.startsWith("run.progression.skillProgression.") &&
                !key.endsWith(".enhancementFailstack") &&
                key != "run.world.automationMode" &&
                !key.startsWith("run.world.selectedFarmEncounterId.") &&
                key != "run.world.pushFailurePolicy" &&
                !key.startsWith("run.world.clearedEncounterIds.")
        )
        val migrated = SaveMigrationRegistry().migrate(
            SaveEnvelope(SaveVersion.V3, SimulationTestSupport.CONTENT_VERSION, 5_602_000L, SaveData(v3Fields))
        )
        check(migrated.schemaVersion == SaveVersion.CURRENT)
        check(migrated.gameState() == current)

        val progressed = current.copy(run = current.run.copy(world = current.run.world.copy(
            activeRegionId = DefaultGameContent.TRAINING_HOLLOW_REGION_ID,
            unlockedRegionIds = setOf(DefaultGameContent.TRAINING_HOLLOW_REGION_ID),
            regionProgressById = mapOf(
                DefaultGameContent.TRAINING_HOLLOW_REGION_ID to RegionProgressState(
                    highestClearedEncounterTier = 3L,
                    normalClears = GameNumber.of(3L)
                )
            )
        )))
        val v4Fields = SaveData.fromGameState(progressed).fields.filterKeys { key ->
            !key.startsWith("run.player.selectedSkillEvolutionBySkillId.") &&
                !key.startsWith("meta.heroName.") &&
                !key.startsWith("meta.rebirth.") &&
                !key.startsWith("run.progression.skillProgression.") &&
                !key.endsWith(".enhancementFailstack") &&
                key != "run.world.automationMode" &&
                !key.startsWith("run.world.selectedFarmEncounterId.") &&
                key != "run.world.pushFailurePolicy" &&
                !key.startsWith("run.world.clearedEncounterIds.")
        }
        val migratedProgress = SaveMigrationRegistry().migrate(
            SaveEnvelope(SaveVersion.V4, SimulationTestSupport.CONTENT_VERSION, 5_603_000L, SaveData(v4Fields))
        ).gameState()
        check(migratedProgress.run.world.clearedEncounterIds == region.encounterIds.take(3).toSet())

        val hostile = progressed.copy(run = progressed.run.copy(world = progressed.run.world.copy(
            regionProgressById = mapOf(DefaultGameContent.TRAINING_HOLLOW_REGION_ID to RegionProgressState(
                highestClearedEncounterTier = 999L, normalClears = GameNumber.of(999L))),
            currentEncounter = EncounterState(region.encounterIds.last(), 30L, 77L, status = EncounterStatus.ACTIVE))))
        val hostileV4 = SaveData.fromGameState(hostile).fields.filterKeys { key ->
            !key.startsWith("run.player.selectedSkillEvolutionBySkillId.") &&
                !key.startsWith("meta.heroName.") &&
                !key.startsWith("meta.rebirth.") &&
                !key.startsWith("run.progression.skillProgression.") &&
                !key.endsWith(".enhancementFailstack") &&
                key != "run.world.automationMode" &&
                !key.startsWith("run.world.selectedFarmEncounterId.") &&
                key != "run.world.pushFailurePolicy" &&
                !key.startsWith("run.world.clearedEncounterIds.")
        }
        val hostileMigrated = SaveMigrationRegistry().migrate(SaveEnvelope(
            SaveVersion.V4, SimulationTestSupport.CONTENT_VERSION, 5_604_000L, SaveData(hostileV4))).gameState()
        check(hostileMigrated.run.world.clearedEncounterIds == region.encounterIds.take(8).toSet())
    }
}
