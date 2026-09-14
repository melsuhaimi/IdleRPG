package com.idlerpg.game.domain.model.quest

import com.idlerpg.game.core.id.ContentId
import com.idlerpg.game.core.number.GameNumber

/**
 * Canonical progress for one authored run quest.
 *
 * Objective progress describes the current cycle. Repeatable completions restart that
 * cycle while completionCount - claimedCount banks all outstanding rewards.
 */
data class QuestProgressState(
    val progressByObjectiveId: Map<ContentId, GameNumber> = emptyMap(),
    val completionCount: GameNumber = GameNumber.ZERO,
    val claimedCount: GameNumber = GameNumber.ZERO
) {
    init {
        require(completionCount >= GameNumber.ZERO) {
            "Quest completionCount cannot be negative: $completionCount"
        }
        require(claimedCount >= GameNumber.ZERO) {
            "Quest claimedCount cannot be negative: $claimedCount"
        }
        require(claimedCount <= completionCount) {
            "Quest claimedCount cannot exceed completionCount: $claimedCount > $completionCount"
        }
    }
}
