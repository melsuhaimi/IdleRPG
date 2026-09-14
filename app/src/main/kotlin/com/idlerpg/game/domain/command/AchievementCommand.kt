package com.idlerpg.game.domain.command

import com.idlerpg.game.core.id.ContentId

/** Explicit player intent to collect a completed persistent-achievement reward. */
sealed interface AchievementCommand : GameCommand

data class ClaimAchievementReward(
    val achievementId: ContentId,
    override val correlationId: CommandCorrelationId? = null
) : AchievementCommand
