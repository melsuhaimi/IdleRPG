package com.idlerpg.game.domain.event

import com.idlerpg.game.core.id.ContentId
import com.idlerpg.game.core.number.GameNumber

/** Completed player-progression facts. */
sealed interface ProgressionEvent : GameEvent

data class ExperienceGranted(
    val amount: GameNumber,
    val sourceId: ContentId? = null
) : ProgressionEvent

data class PlayerLeveledUp(
    val previousLevel: Long,
    val newLevel: Long
) : ProgressionEvent {
    init {
        require(previousLevel > 0L) { "previousLevel must be positive: $previousLevel" }
        require(newLevel > previousLevel) {
            "newLevel must exceed previousLevel: $previousLevel -> $newLevel"
        }
    }
}

data class FeatureUnlocked(
    val featureId: ContentId
) : ProgressionEvent

data class MasteryIncreased(
    val affinityId: ContentId,
    val amount: GameNumber
) : ProgressionEvent
