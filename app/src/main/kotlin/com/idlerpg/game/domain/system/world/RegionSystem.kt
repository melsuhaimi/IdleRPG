package com.idlerpg.game.domain.system.world

import com.idlerpg.game.core.id.ContentId
import com.idlerpg.game.core.number.GameNumber
import com.idlerpg.game.data.content.ContentRegistry
import com.idlerpg.game.domain.definition.world.EncounterDefinition
import com.idlerpg.game.domain.definition.world.EncounterType
import com.idlerpg.game.domain.event.GameEvent
import com.idlerpg.game.domain.event.RegionSelected
import com.idlerpg.game.domain.event.RegionUnlocked
import com.idlerpg.game.domain.model.GameState
import com.idlerpg.game.domain.model.combat.CombatState
import com.idlerpg.game.domain.model.world.RegionProgressState

/** State/events from one deterministic region transition. */
data class RegionTransitionResult(
    val state: GameState,
    val events: List<GameEvent>
)

/** State/events from recording one completed encounter in a region. */
data class RegionClearResult(
    val state: GameState,
    val events: List<GameEvent>
)

/** Owns run-level region selection, unlock initialization, and clear counters. */
object RegionSystem {

    fun selectRegion(
        state: GameState,
        regionId: ContentId,
        unlockStartingRegion: Boolean
    ): RegionTransitionResult {
        val world = state.run.world
        val alreadyUnlocked = regionId in world.unlockedRegionIds

        val unlockedIds =
            if (alreadyUnlocked || !unlockStartingRegion) {
                world.unlockedRegionIds
            } else {
                world.unlockedRegionIds + regionId
            }

        val progress =
            world.regionProgressById[regionId] ?: RegionProgressState()

        val updatedWorld = world.copy(
            activeRegionId = regionId,
            unlockedRegionIds = unlockedIds,
            regionProgressById = world.regionProgressById + (regionId to progress),
            currentEncounter = null
        )

        val updatedState = state.copy(
            run = state.run.copy(
                world = updatedWorld,
                combat = CombatState(
                    combatSequenceId = state.run.combat.combatSequenceId
                )
            )
        )

        return RegionTransitionResult(
            state = updatedState,
            events = buildList {
                if (!alreadyUnlocked && unlockStartingRegion) {
                    add(RegionUnlocked(regionId))
                }
                add(RegionSelected(regionId))
            }
        )
    }

    fun recordEncounterClear(
        state: GameState,
        encounterDefinition: EncounterDefinition,
        encounterIndex: Long,
        contentRegistry: ContentRegistry
    ): RegionClearResult {
        require(encounterIndex > 0L) {
            "encounterIndex must be positive: $encounterIndex"
        }

        val regionId = encounterDefinition.regionId
        val regionDefinition = contentRegistry.region(regionId)
        val stageOrdinal = regionDefinition.encounterIds.indexOf(encounterDefinition.id)
            .takeIf { it >= 0 }
            ?.plus(1)
            ?.toLong()
            ?: error("Encounter ${encounterDefinition.id} is not ordered in region $regionId")
        val current = state.run.world.regionProgressById[regionId]
            ?: RegionProgressState()

        var updated = current.copy(
            highestClearedEncounterTier = maxOf(
                current.highestClearedEncounterTier,
                stageOrdinal
            )
        )

        updated = when (encounterDefinition.type) {
            EncounterType.NORMAL ->
                updated.copy(
                    normalClears = updated.normalClears + GameNumber.ONE
                )

            EncounterType.ELITE ->
                updated.copy(
                    eliteClears = updated.eliteClears + GameNumber.ONE
                )

            EncounterType.ANOMALY,
            EncounterType.BOSS -> updated
        }

        val bossResolution = BossSystem.recordBossDefeat(
            progress = updated,
            encounterDefinition = encounterDefinition,
            contentRegistry = contentRegistry
        )
        updated = bossResolution.first

        val unlockEvents = BossSystem.newlyUnlockedEvents(
            regionId = regionId,
            bossIds = regionDefinition.bossIds,
            before = current,
            after = updated,
            contentRegistry = contentRegistry
        )

        val world = state.run.world.copy(
            regionProgressById =
                state.run.world.regionProgressById + (regionId to updated),
            clearedEncounterIds = state.run.world.clearedEncounterIds + encounterDefinition.id
        )

        return RegionClearResult(
            state = state.copy(
                run = state.run.copy(
                    world = world
                )
            ),
            events = bossResolution.second + unlockEvents
        )
    }
}
