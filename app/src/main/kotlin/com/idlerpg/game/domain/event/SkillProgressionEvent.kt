package com.idlerpg.game.domain.event

import com.idlerpg.game.core.id.ContentId
import com.idlerpg.game.core.number.GameNumber

/** Completed facts for skill rank, mastery, and refinement investment. */
sealed interface SkillProgressionEvent : SkillEvent

data class SkillRankIncreased(
    val skillId: ContentId,
    val previousRank: Long,
    val newRank: Long,
    val totalCost: GameNumber
) : SkillProgressionEvent {
    init {
        require(previousRank >= 1L)
        require(newRank == previousRank + 1L)
        require(totalCost > GameNumber.ZERO)
    }
}

data class SkillMasteryIncreased(
    val skillId: ContentId,
    val previousMastery: Long,
    val newMastery: Long,
    val totalCost: GameNumber
) : SkillProgressionEvent {
    init {
        require(previousMastery >= 0L)
        require(newMastery == previousMastery + 1L)
        require(totalCost > GameNumber.ZERO)
    }
}

data class SkillRefinementIncreased(
    val skillId: ContentId,
    val previousRefinement: Long,
    val newRefinement: Long,
    val totalCost: GameNumber
) : SkillProgressionEvent {
    init {
        require(previousRefinement >= 0L)
        require(newRefinement == previousRefinement + 1L)
        require(totalCost > GameNumber.ZERO)
    }
}
