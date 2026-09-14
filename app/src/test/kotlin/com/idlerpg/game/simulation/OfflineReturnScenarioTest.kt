package com.idlerpg.game.simulation

import com.idlerpg.game.application.OfflineSessionCoordinator
import com.idlerpg.game.application.OfflineTacticalInsight
import com.idlerpg.game.core.id.InstanceId
import com.idlerpg.game.core.number.GameNumber
import com.idlerpg.game.core.time.GameDuration
import com.idlerpg.game.core.time.GameTime
import com.idlerpg.game.data.content.DefaultGameContent
import com.idlerpg.game.data.content.HollowWardenContent
import com.idlerpg.game.data.content.TrainingHollowLootContent
import com.idlerpg.game.data.content.TrainingHollowWorldContent
import com.idlerpg.game.domain.definition.Affinity
import com.idlerpg.game.domain.definition.CurrencyId
import com.idlerpg.game.domain.definition.Rarity
import com.idlerpg.game.domain.event.BossDefeated
import com.idlerpg.game.domain.event.BossPhaseChanged
import com.idlerpg.game.domain.event.AdaptationTierChanged
import com.idlerpg.game.domain.event.CurrencyGranted
import com.idlerpg.game.domain.event.EncounterCleared
import com.idlerpg.game.domain.event.EncounterFailed
import com.idlerpg.game.domain.event.ExperienceGranted
import com.idlerpg.game.domain.event.GameEvent
import com.idlerpg.game.domain.event.GameEventEnvelope
import com.idlerpg.game.domain.event.ItemAdded
import com.idlerpg.game.domain.event.ItemAutoSalvaged
import com.idlerpg.game.domain.event.ItemDropped
import com.idlerpg.game.domain.event.ItemSentToOverflow
import com.idlerpg.game.domain.event.MasteryIncreased
import com.idlerpg.game.domain.model.GameState
import com.idlerpg.game.domain.model.world.EncounterState
import com.idlerpg.game.domain.model.world.EncounterStatus
import com.idlerpg.game.domain.model.world.WorldState

/** Offline Return 2.0 derives rich, deterministic information without granting rewards. */
object OfflineReturnScenarioTest {
    fun run() {
        val factory = SimulationTestSupport.factory()
        val coordinator = OfflineSessionCoordinator(
            repository = SimulationTestSupport.InMemoryGameRepository(),
            clock = SimulationTestSupport.MutableClock(1L),
            engineContext = factory.createEngineContext()
        )
        val regionId = DefaultGameContent.TRAINING_HOLLOW_REGION_ID
        fun stateAt(clearedStage: Int): GameState = GameState.newGame(901L).copy(
            run = GameState.newGame(901L).run.copy(
                world = WorldState(
                    unlockedRegionIds = setOf(regionId),
                    clearedEncounterIds = setOf(TrainingHollowWorldContent.stageId(clearedStage))
                )
            )
        )
        var sequence = 0L
        fun envelope(event: GameEvent) = GameEventEnvelope(
            sequenceNumber = ++sequence,
            simulationTime = GameTime.ofMillis(sequence),
            event = event
        )

        val events = listOf(
            envelope(EncounterCleared(TrainingHollowWorldContent.stageId(10), 10L)),
            envelope(EncounterCleared(TrainingHollowWorldContent.stageId(12), 12L)),
            envelope(EncounterCleared(TrainingHollowWorldContent.stageId(30), 30L)),
            envelope(EncounterCleared(TrainingHollowWorldContent.stageId(30), 31L)),
            envelope(BossDefeated(HollowWardenContent.BOSS_ID, regionId)),
            envelope(CurrencyGranted(CurrencyId.GOLD, GameNumber.of(500L))),
            envelope(ExperienceGranted(GameNumber.of(300L))),
            envelope(MasteryIncreased(Affinity.EMBER.id, GameNumber.of(25L))),
            envelope(ItemDropped(InstanceId(101L), TrainingHollowLootContent.ACCESSORY_ITEM_ID, Rarity.EPIC)),
            envelope(ItemAdded(InstanceId(101L), TrainingHollowLootContent.ACCESSORY_ITEM_ID)),
            envelope(ItemDropped(InstanceId(102L), TrainingHollowLootContent.ARMOR_ITEM_ID, Rarity.RARE)),
            envelope(ItemSentToOverflow(InstanceId(102L), TrainingHollowLootContent.ARMOR_ITEM_ID)),
            envelope(ItemDropped(InstanceId(104L), TrainingHollowLootContent.HELM_ITEM_ID, Rarity.RARE)),
            envelope(ItemAdded(InstanceId(104L), TrainingHollowLootContent.HELM_ITEM_ID)),
            envelope(ItemDropped(InstanceId(105L), TrainingHollowLootContent.BOOTS_ITEM_ID, Rarity.RARE)),
            envelope(ItemAdded(InstanceId(105L), TrainingHollowLootContent.BOOTS_ITEM_ID)),
            envelope(ItemDropped(InstanceId(106L), DefaultGameContent.TRAINING_CATALYST_ITEM_ID, Rarity.LEGENDARY)),
            envelope(ItemAdded(InstanceId(106L), DefaultGameContent.TRAINING_CATALYST_ITEM_ID)),
            envelope(ItemDropped(InstanceId(103L), DefaultGameContent.TRAINING_BLADE_ITEM_ID, Rarity.COMMON)),
            envelope(ItemAutoSalvaged(
                InstanceId(103L),
                DefaultGameContent.TRAINING_BLADE_ITEM_ID,
                Rarity.COMMON,
                GameNumber.of(7L)
            )),
            envelope(AdaptationTierChanged(regionId, Affinity.EMBER.id, 0, 1)),
            envelope(EncounterFailed(TrainingHollowWorldContent.stageId(10), 10L))
        )

        val summary = coordinator.summarize(
            requestedElapsed = GameDuration.ofHours(8L),
            simulatedElapsed = GameDuration.ofHours(8L),
            durationClamped = false,
            clockRollbackDetected = false,
            events = events,
            before = stateAt(9),
            after = stateAt(12)
        )

        check(summary.startingStage == 9)
        check(summary.endingStage == 12)
        check(summary.deepestStage == 30)
        check(summary.currentWallStage == 10)
        check(summary.encountersCleared == GameNumber.of(4L))
        check(summary.eliteEncountersCleared == GameNumber.ONE)
        check(summary.anomalyEncountersCleared == GameNumber.ONE)
        check(summary.bossesDefeated == GameNumber.of(2L))
        check(summary.adaptationTierChanges == GameNumber.ONE)
        check(summary.masteryGranted == GameNumber.of(25L))
        check(summary.itemsFound == GameNumber.of(6L))
        check(summary.itemsKept == GameNumber.of(4L))
        check(summary.itemsOverflowed == GameNumber.ONE)
        check(summary.itemsAutoSalvaged == GameNumber.ONE)
        check(summary.autoSalvageGold == GameNumber.of(7L))
        check(summary.notableDrops.map { it.rarity } == listOf(Rarity.LEGENDARY, Rarity.EPIC, Rarity.RARE))
        check(summary.tacticalInsight == OfflineTacticalInsight.PROTECTOR_BLOCKING)

        val replay = coordinator.summarize(
            requestedElapsed = GameDuration.ofHours(8L),
            simulatedElapsed = GameDuration.ofHours(8L),
            durationClamped = false,
            clockRollbackDetected = false,
            events = events,
            before = stateAt(9),
            after = stateAt(12)
        )
        check(replay == summary)

        val reflectionSummary = coordinator.summarize(
            requestedElapsed = GameDuration.ofHours(1L),
            simulatedElapsed = GameDuration.ofHours(1L),
            durationClamped = false,
            clockRollbackDetected = false,
            events = listOf(
                envelope(BossPhaseChanged(HollowWardenContent.BOSS_ID, 2, 3)),
                envelope(EncounterFailed(TrainingHollowWorldContent.stageId(30), 30L))
            ),
            before = stateAt(29),
            // Automation may already have moved away from the failed boss after this event.
            after = stateAt(12)
        )
        check(reflectionSummary.tacticalInsight == OfflineTacticalInsight.ADAPTIVE_RESISTANCE)

        val midFractureState = stateAt(29).let { state ->
            state.copy(run = state.run.copy(world = state.run.world.copy(
                activeRegionId = regionId,
                currentEncounter = EncounterState(
                    definitionId = TrainingHollowWorldContent.stageId(30),
                    encounterIndex = 30L,
                    encounterSeed = 901L,
                    currentWave = 3,
                    status = EncounterStatus.ACTIVE
                )
            )))
        }
        val resumedFractureSummary = coordinator.summarize(
            requestedElapsed = GameDuration.ofHours(1L),
            simulatedElapsed = GameDuration.ofHours(1L),
            durationClamped = false,
            clockRollbackDetected = false,
            events = listOf(envelope(EncounterFailed(TrainingHollowWorldContent.stageId(30), 30L))),
            before = midFractureState,
            after = stateAt(12)
        )
        check(resumedFractureSummary.tacticalInsight == OfflineTacticalInsight.CASTER_DISRUPTION)
    }
}
