package com.idlerpg.game.domain.system.quest

import com.idlerpg.game.core.number.GameNumber
import com.idlerpg.game.domain.definition.quest.QuestDefinition
import com.idlerpg.game.domain.definition.quest.QuestRewardDefinition
import com.idlerpg.game.domain.command.ClaimQuestReward
import com.idlerpg.game.domain.command.CommandRejectionCode
import com.idlerpg.game.domain.command.CommandRejectionReason
import com.idlerpg.game.domain.engine.CommandHandlingResult
import com.idlerpg.game.domain.engine.EngineContext
import com.idlerpg.game.domain.event.QuestRewardClaimed
import com.idlerpg.game.domain.model.GameState
import com.idlerpg.game.domain.system.reward.CurrencyReward
import com.idlerpg.game.domain.system.reward.ExperienceReward
import com.idlerpg.game.domain.system.reward.LootTableReward
import com.idlerpg.game.domain.system.reward.RewardBundle
import com.idlerpg.game.domain.system.reward.RewardSystem

/** Atomic owner of explicit run-quest reward collection. */
object QuestClaimSystem {
    /** Constant-size reward calculation even after a long unattended session. */
    fun rewardForClaim(state: GameState, definition: QuestDefinition): QuestRewardDefinition {
        if (!definition.repeatable) return definition.reward
        val progress = state.run.quests.progressFor(definition.id)
        val pending = progress.completionCount - progress.claimedCount
        val cycles = if (pending > GameNumber.ZERO) pending else GameNumber.ONE
        fun scale(amount: GameNumber) = GameNumber.fromBigInteger(
            amount.toBigInteger().multiply(cycles.toBigInteger())
        )
        return definition.reward.copy(
            currencies = definition.reward.currencies.mapValues { scale(it.value) },
            experience = scale(definition.reward.experience)
        )
    }

    fun handle(state: GameState, command: ClaimQuestReward, context: EngineContext): CommandHandlingResult {
        val definition = context.contentRegistry.questOrNull(command.questId) ?:
            return CommandHandlingResult.Rejected(CommandRejectionReason(CommandRejectionCode.UNKNOWN_CONTENT, subjectContentId = command.questId))
        if (!QuestSystem.isEligible(state, definition)) {
            return CommandHandlingResult.Rejected(CommandRejectionReason(CommandRejectionCode.LOCKED, subjectContentId = command.questId))
        }
        val current = state.run.quests.progressFor(command.questId)
        if (current.completionCount == GameNumber.ZERO) {
            return CommandHandlingResult.Rejected(CommandRejectionReason(CommandRejectionCode.NOT_READY, subjectContentId = command.questId))
        }
        if (current.claimedCount >= current.completionCount) {
            return CommandHandlingResult.Rejected(CommandRejectionReason(CommandRejectionCode.ALREADY_CLAIMED, subjectContentId = command.questId))
        }
        val reward = rewardForClaim(state, definition)
        val bundle = RewardBundle(
                currencies = reward.currencies.entries.sortedBy { it.key }.filter { it.value > GameNumber.ZERO }.map {
                    CurrencyReward(it.key, it.value, definition.id)
                },
                experience = if (reward.experience > GameNumber.ZERO) listOf(ExperienceReward(reward.experience, definition.id)) else emptyList(),
                lootTables = reward.lootTableIds.sorted().map { LootTableReward(it, definition.id) }
        )
        val grant = RewardSystem.grant(state, bundle, context)
        val nextClaimed = if (definition.repeatable) current.completionCount else current.claimedCount + GameNumber.ONE
        val progress = grant.state.run.quests.progressFor(command.questId).copy(claimedCount = nextClaimed)
        val next = grant.state.copy(run = grant.state.run.copy(quests = grant.state.run.quests.copy(
            progressByQuestId = grant.state.run.quests.progressByQuestId + (command.questId to progress)
        )))
        return CommandHandlingResult.Accepted(next, grant.events + QuestRewardClaimed(command.questId, nextClaimed))
    }
}
