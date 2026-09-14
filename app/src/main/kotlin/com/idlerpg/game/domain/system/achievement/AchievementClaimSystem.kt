package com.idlerpg.game.domain.system.achievement

import com.idlerpg.game.core.number.GameNumber
import com.idlerpg.game.domain.command.ClaimAchievementReward
import com.idlerpg.game.domain.command.CommandRejectionCode
import com.idlerpg.game.domain.command.CommandRejectionReason
import com.idlerpg.game.domain.engine.CommandHandlingResult
import com.idlerpg.game.domain.engine.EngineContext
import com.idlerpg.game.domain.event.AchievementRewardClaimed
import com.idlerpg.game.domain.model.GameState
import com.idlerpg.game.domain.system.reward.CurrencyReward
import com.idlerpg.game.domain.system.reward.ExperienceReward
import com.idlerpg.game.domain.system.reward.LootTableReward
import com.idlerpg.game.domain.system.reward.RewardBundle
import com.idlerpg.game.domain.system.reward.RewardSystem

/** Atomic owner of explicit persistent-achievement reward collection. */
object AchievementClaimSystem {
    fun handle(state: GameState, command: ClaimAchievementReward, context: EngineContext): CommandHandlingResult {
        val definition = context.contentRegistry.achievementOrNull(command.achievementId) ?:
            return CommandHandlingResult.Rejected(CommandRejectionReason(CommandRejectionCode.UNKNOWN_CONTENT, subjectContentId = command.achievementId))
        if (!AchievementSystem.isEligible(state, definition)) {
            return CommandHandlingResult.Rejected(CommandRejectionReason(CommandRejectionCode.LOCKED, subjectContentId = command.achievementId))
        }
        val current = state.meta.achievements.progressFor(command.achievementId)
        if (!current.completed) {
            return CommandHandlingResult.Rejected(CommandRejectionReason(CommandRejectionCode.NOT_READY, subjectContentId = command.achievementId))
        }
        if (current.rewardClaimed) {
            return CommandHandlingResult.Rejected(CommandRejectionReason(CommandRejectionCode.ALREADY_CLAIMED, subjectContentId = command.achievementId))
        }
        val reward = definition.reward
        val bundle = RewardBundle(
                currencies = reward.currencies.entries.sortedBy { it.key }.filter { it.value > GameNumber.ZERO }.map {
                    CurrencyReward(it.key, it.value, definition.id)
                },
                experience = if (reward.experience > GameNumber.ZERO) listOf(ExperienceReward(reward.experience, definition.id)) else emptyList(),
                lootTables = reward.lootTableIds.sorted().map { LootTableReward(it, definition.id) }
        )
        val grant = RewardSystem.grant(state, bundle, context)
        val progress = grant.state.meta.achievements.progressFor(command.achievementId).copy(rewardClaimed = true)
        val next = grant.state.copy(meta = grant.state.meta.copy(achievements = grant.state.meta.achievements.copy(
            progressByAchievementId = grant.state.meta.achievements.progressByAchievementId + (command.achievementId to progress)
        )))
        return CommandHandlingResult.Accepted(next, grant.events + AchievementRewardClaimed(command.achievementId))
    }
}
