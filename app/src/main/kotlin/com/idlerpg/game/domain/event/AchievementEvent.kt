package com.idlerpg.game.domain.event

import com.idlerpg.game.core.id.ContentId

/** Completed facts for persistent-achievement completion and explicit reward collection. */
sealed interface AchievementEvent : GameEvent

data class AchievementCompleted(
    val achievementId: ContentId
) : AchievementEvent

data class AchievementRewardClaimed(
    val achievementId: ContentId
) : AchievementEvent
