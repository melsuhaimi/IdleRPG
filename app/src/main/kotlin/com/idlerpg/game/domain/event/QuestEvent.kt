package com.idlerpg.game.domain.event

import com.idlerpg.game.core.id.ContentId
import com.idlerpg.game.core.number.GameNumber

/** Completed facts for run-quest completion and explicit reward collection. */
sealed interface QuestEvent : GameEvent

data class QuestCompleted(
    val questId: ContentId,
    val completionCount: GameNumber
) : QuestEvent {
    init {
        require(completionCount > GameNumber.ZERO) {
            "QuestCompleted.completionCount must be positive: $completionCount"
        }
    }
}

data class QuestRewardClaimed(
    val questId: ContentId,
    val claimedCount: GameNumber
) : QuestEvent {
    init {
        require(claimedCount > GameNumber.ZERO) {
            "QuestRewardClaimed.claimedCount must be positive: $claimedCount"
        }
    }
}
