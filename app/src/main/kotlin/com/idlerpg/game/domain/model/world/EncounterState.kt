package com.idlerpg.game.domain.model.world

import com.idlerpg.game.core.id.ContentId
import com.idlerpg.game.core.id.InstanceId

/** Lifecycle state for one explicit world encounter. */
enum class EncounterStatus {
    ACTIVE,
    CLEARED,
    FAILED,
    RETREATED
}

/**
 * Current encounter runtime state.
 *
 * The encounter seed is stored as deterministic context; the future encounter system
 * decides how it is created and consumed.
 */
data class EncounterState(
    val definitionId: ContentId,
    val encounterIndex: Long,
    val encounterSeed: Long,
    val currentWave: Int = 1,
    val spawnedEnemyIds: List<InstanceId> = emptyList(),
    val status: EncounterStatus = EncounterStatus.ACTIVE,
    val rewardEligible: Boolean = true
) {
    init {
        require(encounterIndex >= 0L) { "encounterIndex cannot be negative: $encounterIndex" }
        require(currentWave > 0) { "currentWave must be positive: $currentWave" }
        require(spawnedEnemyIds.size == spawnedEnemyIds.toSet().size) {
            "spawnedEnemyIds cannot contain duplicates"
        }
    }
}
