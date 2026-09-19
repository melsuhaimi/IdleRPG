package com.idlerpg.game.simulation

import com.idlerpg.game.application.GameRuntime
import com.idlerpg.game.core.config.BalanceConfig
import com.idlerpg.game.core.number.GameNumber
import com.idlerpg.game.core.time.GameDuration
import com.idlerpg.game.data.content.DefaultGameContent
import com.idlerpg.game.domain.command.ClaimQuestReward
import com.idlerpg.game.domain.definition.CurrencyId
import com.idlerpg.game.domain.event.AdaptationTierChanged
import com.idlerpg.game.domain.event.ConvergenceTriggered
import com.idlerpg.game.domain.event.CurrencyGranted
import com.idlerpg.game.domain.event.EncounterCleared
import com.idlerpg.game.domain.event.EnemyKilled
import com.idlerpg.game.domain.event.ExperienceGranted
import com.idlerpg.game.domain.event.GameEventEnvelope
import com.idlerpg.game.domain.event.ItemAdded
import com.idlerpg.game.domain.event.ItemAutoSalvaged
import com.idlerpg.game.domain.event.ItemSentToOverflow
import com.idlerpg.game.domain.event.ResonanceGenerated

/** Compact aggregate intended for deterministic balance comparisons, not canonical state. */
data class BalanceSimulationReport(
    val simulatedDuration: GameDuration,
    val enemiesDefeated: Long,
    val encountersCleared: Long,
    val goldGranted: GameNumber,
    val experienceGranted: GameNumber,
    val itemsFound: Long,
    val itemsAutoSalvaged: Long,
    val autoSalvageGold: GameNumber,
    val convergencesTriggered: Long,
    val adaptationTierChanges: Long,
    val affinityGenerationById: Map<com.idlerpg.game.core.id.ContentId, GameNumber>,
    val finalState: com.idlerpg.game.domain.model.GameState
)

/**
 * Test/balance harness that advances only GameRuntime and aggregates completed events.
 *
 * Chunking bounds memory while preserving the exact canonical simulation path.
 */
object BalanceSimulationHarness {
    fun simulate(
        runtime: GameRuntime,
        duration: GameDuration,
        chunk: GameDuration = GameDuration.ofMinutes(15L)
    ): BalanceSimulationReport {
        require(chunk > GameDuration.ZERO)

        var remaining = duration.millis
        var kills = 0L
        var clears = 0L
        var gold = GameNumber.ZERO
        var xp = GameNumber.ZERO
        var items = 0L
        var autoSalvaged = 0L
        var autoSalvageGold = GameNumber.ZERO
        var convergences = 0L
        var tierChanges = 0L
        val affinity = sortedMapOf<com.idlerpg.game.core.id.ContentId, GameNumber>()

        fun observe(events: List<GameEventEnvelope>) {
            for (envelope in events) {
                when (val event = envelope.event) {
                    is EnemyKilled -> kills = Math.addExact(kills, 1L)
                    is EncounterCleared -> clears = Math.addExact(clears, 1L)
                    is CurrencyGranted ->
                        if (event.currencyId == CurrencyId.GOLD) {
                            gold = gold + event.amount
                        }
                    is ExperienceGranted -> xp = xp + event.amount
                    is ItemAdded -> items = Math.addExact(items, 1L)
                    is ItemSentToOverflow -> items = Math.addExact(items, 1L)
                    is ItemAutoSalvaged -> {
                        autoSalvaged = Math.addExact(autoSalvaged, 1L)
                        autoSalvageGold += event.goldGranted
                    }
                    is ConvergenceTriggered ->
                        convergences = Math.addExact(convergences, 1L)
                    is AdaptationTierChanged ->
                        tierChanges = Math.addExact(tierChanges, 1L)
                    is ResonanceGenerated -> {
                        affinity[event.affinityId] =
                            (affinity[event.affinityId] ?: GameNumber.ZERO) + event.amount
                    }
                    else -> Unit
                }
            }
        }

        while (remaining > 0L) {
            val step = minOf(remaining, chunk.millis)
            observe(runtime.advance(GameDuration.ofMillis(step)).events)
            remaining -= step
        }

        return BalanceSimulationReport(
            simulatedDuration = duration,
            enemiesDefeated = kills,
            encountersCleared = clears,
            goldGranted = gold,
            experienceGranted = xp,
            itemsFound = items,
            itemsAutoSalvaged = autoSalvaged,
            autoSalvageGold = autoSalvageGold,
            convergencesTriggered = convergences,
            adaptationTierChanges = tierChanges,
            affinityGenerationById = affinity.toMap(),
            finalState = runtime.state()
        )
    }
}

/** Long-run reproducibility and balance-harness smoke test. */
object LongRunSimulationTest {
    fun run(duration: GameDuration = GameDuration.ofHours(24L)): BalanceSimulationReport {
        fun scenario(): BalanceSimulationReport {
            val runtime = SimulationTestSupport.runtime(seed = 404L)
            SimulationTestSupport.startTraining(runtime)
            val simulation = BalanceSimulationHarness.simulate(
                runtime = runtime,
                duration = duration
            )
            val claim = runtime.dispatch(
                ClaimQuestReward(DefaultGameContent.FIRST_HUNT_QUEST_ID)
            )
            SimulationTestSupport.checkAccepted(claim)
            val claimedGold = claim.events
                .map { it.event }
                .filterIsInstance<CurrencyGranted>()
                .filter { it.currencyId == CurrencyId.GOLD }
                .fold(GameNumber.ZERO) { total, event -> total + event.amount }
            return simulation.copy(
                goldGranted = simulation.goldGranted + claimedGold,
                finalState = runtime.state()
            )
        }

        val first = scenario()
        val second = scenario()

        check(first == second)
        check(first.enemiesDefeated > 0L)
        check(first.enemiesDefeated >= first.encountersCleared)
        check(first.goldGranted > GameNumber.ZERO)
        check(first.experienceGranted > GameNumber.ZERO)
        check(first.itemsFound > 0L)
        check(first.itemsFound < first.enemiesDefeated)
        check(first.itemsAutoSalvaged > 0L)
        check(first.autoSalvageGold > GameNumber.ZERO)
        val inventory = first.finalState.run.inventory
        check(inventory.itemsById.size <= BalanceConfig.DEFAULT_MAXIMUM_INVENTORY_CAPACITY.toInt())
        check((inventory.itemsById.values + inventory.overflowItemsById.values).all {
            it.rarity.rank >= com.idlerpg.game.domain.definition.Rarity.RARE.rank
        })
        return first
    }
}

/** Runs every executable scenario check except the heavyweight balance smoke. */
fun runScenarioInventory() {
    PrototypeParityTest.run()
    DeterminismTest.run()
    CommandRejectionTest.run()
    EconomyInvariantTest.run()
    CoreGrowthScenarioTest.run()
    AchievementClaimScenarioTest.run()
    WorldProgressionScenarioTest.run()
    EnemyCombatVarietyScenarioTest.run()
    CombatStatContractScenarioTest.run()
    MultiEnemyCombatScenarioTest.run()
    SkillBuildcraftCoreScenarioTest.run()
    ManualSkillQueueScenarioTest.run()
    StrategyEcosystemScenarioTest.run()
    IncrementalProgressionContractTest.run()
    RebirthScenarioTest.run()
    GearEnhancementScenarioTest.run()
    SkillProgressionScenarioTest.run()
    PowerScoreScenarioTest.run()
    LootWorldAutomationScenarioTest.run()
    InventoryCapacityScenarioTest.run()
    SaveV2MigrationTest.run()
    SaveV3WaveMigrationTest.run()
    SaveV10FailstackMigrationTest.run()
    ResonanceScenarioTest.run()
    DefeatedTargetFollowUpScenarioTest.run()
    ProgressionRepairScenarioTest.run()
    RecurringProgressionScenarioTest.run()
    CombatReadabilityScenarioTest.run()
    DoctrineScenarioTest.run()
    DoctrineStrategyScenarioTest.run()
    AdaptationBossChronicleScenarioTest.run()
    AdaptationBehaviorScenarioTest.run()
    HollowWardenPhaseScenarioTest.run()
    SectorVisualProjectionScenarioTest.run()
    ProgressiveDisclosureScenarioTest.run()
    LootPresentationIdentityScenarioTest.run()
    com.idlerpg.game.presentation.BattleHapticPolicyTest.run()
    ContentValidatorRegressionScenarioTest.run()
    NewGameDeploymentScenarioTest.run()
    SkillEvolutionScenarioTest.run()
    SkillLoadoutScenarioTest.run()
    DamageKindIdentityScenarioTest.run()
    OfflineReturnScenarioTest.run()
    QuestClaimScenarioTest.run()
    AdaptationScenarioTest.run()
    LootDeterminismTest.run()
    SaveRoundTripTest.run()
    ActiveOfflineEquivalenceTest.run()
    OfflineBoundaryScenarioTest.run()
    ChronicleResetTest.run()
    EchoShopScenarioTest.run()
    EchoTrainingScenarioTest.run()
    FrontendEnablementIntegrationScenarioTest.run()
    com.idlerpg.game.presentation.BattleIntentMappingTest.main(emptyArray())
    com.idlerpg.game.presentation.BattleProjectionTest.main(emptyArray())
    com.idlerpg.game.presentation.DoctrineDraftControllerTest.main(emptyArray())
    com.idlerpg.game.presentation.DoctrineIntentMappingTest.main(emptyArray())
    com.idlerpg.game.presentation.DoctrineProjectionTest.main(emptyArray())
    com.idlerpg.game.presentation.GearIntentMappingTest.run()
    com.idlerpg.game.presentation.GearProjectionTest.run()
    com.idlerpg.game.presentation.OfflineProgressProjectionTest.main(emptyArray())
    com.idlerpg.game.presentation.PresentationAccessibilityContractTest.main(emptyArray())
    com.idlerpg.game.presentation.PresentationContentCoverageTest.main(emptyArray())
    com.idlerpg.game.presentation.ProgressIntentMappingTest.main(emptyArray())
    com.idlerpg.game.presentation.ProgressProjectionTest.main(emptyArray())
    com.idlerpg.game.presentation.SkillLoadoutIntentMappingTest.main(emptyArray())
    com.idlerpg.game.presentation.SkillLoadoutProjectionTest.main(emptyArray())
    com.idlerpg.game.presentation.WorldIntentMappingTest.main(emptyArray())
    com.idlerpg.game.presentation.WorldProjectionTest.main(emptyArray())
}

/** Runs the complete scenario suite, including the deterministic long-run smoke. */
fun runAllScenarioChecks(): BalanceSimulationReport {
    runScenarioInventory()
    return LongRunSimulationTest.run()
}

fun main() {
    val longRun = runAllScenarioChecks()
    println("FBE05_INTEGRATION_PASS")
    println("longRunMinutes=${longRun.simulatedDuration.millis / 60_000L}")
    println("longRunKills=${longRun.enemiesDefeated}")
    println("longRunGold=${longRun.goldGranted}")
    println("longRunXp=${longRun.experienceGranted}")
    println("longRunItems=${longRun.itemsFound}")
}
