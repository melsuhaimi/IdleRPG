package com.idlerpg.game.domain.model.world

import com.idlerpg.game.core.id.ContentId

enum class WorldAutomationMode { PUSH, FARM }
enum class PushFailurePolicy { RETRY, FALL_BACK_TO_SELECTED_FARM, FARM_HIGHEST_CLEARED }

/** Run-level world selection, unlocks, region progress, and current encounter ownership. */
data class WorldState(
    val activeRegionId: ContentId? = null,
    val unlockedRegionIds: Set<ContentId> = emptySet(),
    val regionProgressById: Map<ContentId, RegionProgressState> = emptyMap(),
    val currentEncounter: EncounterState? = null,
    val milestoneFlagIds: Set<ContentId> = emptySet(),
    val automationMode: WorldAutomationMode = WorldAutomationMode.PUSH,
    val selectedFarmEncounterId: ContentId? = null,
    val pushFailurePolicy: PushFailurePolicy = PushFailurePolicy.FARM_HIGHEST_CLEARED,
    val clearedEncounterIds: Set<ContentId> = emptySet()
) {
    init {
        if (activeRegionId != null) {
            require(activeRegionId in unlockedRegionIds) {
                "activeRegionId must be present in unlockedRegionIds"
            }
        }
        require(regionProgressById.keys.all { it in unlockedRegionIds }) {
            "regionProgressById cannot contain a locked region"
        }
        if (currentEncounter != null) {
            require(activeRegionId != null) {
                "currentEncounter requires an activeRegionId"
            }
        }
        if (selectedFarmEncounterId != null) {
            require(selectedFarmEncounterId in clearedEncounterIds) {
                "selectedFarmEncounterId must reference cleared content"
            }
        }
    }
}
