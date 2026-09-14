package com.idlerpg.game.domain.model.achievement

import com.idlerpg.game.core.id.ContentId
import com.idlerpg.game.core.number.GameNumber

/** Canonical persistent progress for one authored achievement. */
data class AchievementProgressState(
    val progressByObjectiveId: Map<ContentId, GameNumber> = emptyMap(),
    val completed: Boolean = false,
    val rewardClaimed: Boolean = false
) {
    init {
        require(!rewardClaimed || completed) {
            "Achievement reward cannot be claimed before completion"
        }
    }
}
