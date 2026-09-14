package com.idlerpg.game.domain.command

import com.idlerpg.game.core.id.ContentId

/** Explicit player intent to collect a completed run-quest reward. */
sealed interface QuestCommand : GameCommand

data class ClaimQuestReward(
    val questId: ContentId,
    override val correlationId: CommandCorrelationId? = null
) : QuestCommand
