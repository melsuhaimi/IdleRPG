package com.idlerpg.game.domain.event

import com.idlerpg.game.core.id.ContentId
import com.idlerpg.game.core.id.InstanceId

/** Completed world/encounter facts. */
sealed interface WorldEvent : GameEvent

data class RegionUnlocked(
    val regionId: ContentId
) : WorldEvent

data class RegionSelected(
    val regionId: ContentId
) : WorldEvent

data class EncounterStarted(
    val encounterDefinitionId: ContentId,
    val encounterIndex: Long
) : WorldEvent {
    init {
        require(encounterIndex > 0L) {
            "EncounterStarted.encounterIndex must be positive: $encounterIndex"
        }
    }
}

data class EncounterWaveStarted(
    val encounterDefinitionId: ContentId,
    val encounterIndex: Long,
    val wave: Int,
    val totalWaves: Int,
    val enemyInstanceIds: List<InstanceId>
) : WorldEvent {
    init {
        require(encounterIndex > 0L) { "encounterIndex must be positive" }
        require(wave in 1..totalWaves) { "wave must be within totalWaves" }
        require(enemyInstanceIds.isNotEmpty()) { "A wave must spawn enemies" }
        require(enemyInstanceIds.size == enemyInstanceIds.toSet().size) {
            "enemyInstanceIds cannot contain duplicates"
        }
    }
}

data class EncounterCleared(
    val encounterDefinitionId: ContentId,
    val encounterIndex: Long
) : WorldEvent {
    init {
        require(encounterIndex > 0L) {
            "EncounterCleared.encounterIndex must be positive: $encounterIndex"
        }
    }
}

data class EncounterFailed(
    val encounterDefinitionId: ContentId,
    val encounterIndex: Long
) : WorldEvent {
    init {
        require(encounterIndex > 0L) {
            "EncounterFailed.encounterIndex must be positive: $encounterIndex"
        }
    }
}

data class EncounterRetreated(
    val encounterDefinitionId: ContentId,
    val encounterIndex: Long
) : WorldEvent {
    init {
        require(encounterIndex > 0L) {
            "EncounterRetreated.encounterIndex must be positive: $encounterIndex"
        }
    }
}

data class BossUnlocked(
    val bossId: ContentId,
    val regionId: ContentId
) : WorldEvent

data class BossDefeated(
    val bossId: ContentId,
    val regionId: ContentId
) : WorldEvent
data class BossPhaseChanged(val bossId: ContentId, val phase: Int, val totalPhases: Int) : WorldEvent
data class BossFirstClearRewardGranted(val bossId: ContentId, val lootTableId: ContentId) : WorldEvent
