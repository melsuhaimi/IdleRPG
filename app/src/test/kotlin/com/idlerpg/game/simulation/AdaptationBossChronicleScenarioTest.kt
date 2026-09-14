package com.idlerpg.game.simulation

import com.idlerpg.game.core.number.GameNumber
import com.idlerpg.game.data.content.DefaultGameContent
import com.idlerpg.game.data.content.HollowWardenContent
import com.idlerpg.game.data.content.TrainingHollowWorldContent
import com.idlerpg.game.domain.definition.Affinity
import com.idlerpg.game.domain.definition.CurrencyId
import com.idlerpg.game.domain.definition.world.EncounterType
import com.idlerpg.game.domain.event.BossDefeated
import com.idlerpg.game.domain.event.BossFirstClearRewardGranted
import com.idlerpg.game.domain.model.MetaState
import com.idlerpg.game.domain.model.progression.FeatureUnlockState
import com.idlerpg.game.domain.model.world.RegionProgressState
import com.idlerpg.game.domain.system.chronicle.ChronicleSystem
import com.idlerpg.game.domain.system.state.RunStateFactory
import com.idlerpg.game.domain.system.world.BossSystem
import com.idlerpg.game.domain.model.GameState
import com.idlerpg.game.presentation.query.DefaultGameReadQueries

object AdaptationBossChronicleScenarioTest {
    fun run() {
        val factory = SimulationTestSupport.factory()
        val registry = factory.contentRegistry
        val region = registry.region(DefaultGameContent.TRAINING_HOLLOW_REGION_ID)
        check(region.adaptationMutationIds.size == 8)
        check(region.adaptationMutationIds.map { registry.mutation(it).triggerAffinity }.toSet() == Affinity.values().toSet())
        val encounter = registry.encounter(TrainingHollowWorldContent.stageId(30))
        check(encounter.type == EncounterType.BOSS && encounter.waves == 3)
        check(encounter.bossId == HollowWardenContent.BOSS_ID)
        val initial = RegionProgressState(normalClears = GameNumber.of(15L), highestClearedEncounterTier = 30L)
        val first = BossSystem.recordBossDefeat(initial, encounter, registry)
        check(first.second.any { it is BossDefeated } && first.second.any { it is BossFirstClearRewardGranted })
        check(BossSystem.recordBossDefeat(first.first, encounter, registry).second.isEmpty())
        val runtime = SimulationTestSupport.runtime(910L)
        SimulationTestSupport.makeChronicleEligible(runtime)
        val definition = registry.defaultChronicleDefinition()
        check(ChronicleSystem.isEligible(runtime.state(), definition))
        check(ChronicleSystem.echoReward(runtime.state(), definition) == GameNumber.of(35L))
        val meta = MetaState(persistentFeatureUnlocks = FeatureUnlockState(setOf(DefaultGameContent.LEGACY_ACCELERATION_FEATURE_ID)))
        val fresh = RunStateFactory.fresh(factory.gameConfig.balance, meta)
        check(fresh.economy.wallet.amountsByCurrencyId[CurrencyId.GOLD] == GameNumber.of(100L))
        val capabilityMeta = MetaState(
            persistentFeatureUnlocks = FeatureUnlockState(
                setOf(
                    DefaultGameContent.DOCTRINE_MEMORY_FEATURE_ID,
                    DefaultGameContent.EXPEDITION_MEMORY_FEATURE_ID
                )
            )
        )
        val accelerated = RunStateFactory.fresh(factory.gameConfig.balance, capabilityMeta)
        check(accelerated.world.activeRegionId == DefaultGameContent.TRAINING_HOLLOW_REGION_ID)
        check(accelerated.world.clearedEncounterIds.size == 3)
        check(accelerated.world.regionProgressById.getValue(DefaultGameContent.TRAINING_HOLLOW_REGION_ID).highestClearedEncounterTier == 3L)
        val capabilityState = GameState(run = accelerated, meta = capabilityMeta)
        val queries = DefaultGameReadQueries(
            registry,
            factory.gameConfig.balance,
            factory.createEngineContext()
        )
        check(queries.doctrineCapacity(capabilityState) ==
            factory.gameConfig.balance.baseDoctrineRuleCapacity + 1)
    }
}
