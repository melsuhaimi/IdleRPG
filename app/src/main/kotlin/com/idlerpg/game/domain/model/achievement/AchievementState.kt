package com.idlerpg.game.domain.model.achievement

import com.idlerpg.game.core.id.ContentId

/** Chronicle-persistent aggregate of achievement progress/completion. */
data class AchievementState(
    val progressByAchievementId: Map<ContentId, AchievementProgressState> = emptyMap()
) {
    fun progressFor(achievementId: ContentId): AchievementProgressState =
        progressByAchievementId[achievementId] ?: AchievementProgressState()
}
