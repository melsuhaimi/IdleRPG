package com.idlerpg.game.simulation

import com.idlerpg.game.core.id.InstanceId
import com.idlerpg.game.core.number.GameNumber
import com.idlerpg.game.data.content.DefaultGameContent
import com.idlerpg.game.data.content.TrainingHollowQuestContent
import com.idlerpg.game.data.local.SaveCodec
import com.idlerpg.game.data.local.SaveEnvelope
import com.idlerpg.game.domain.command.ClaimQuestReward
import com.idlerpg.game.domain.engine.CommandHandlingResult
import com.idlerpg.game.domain.definition.Affinity
import com.idlerpg.game.domain.event.EnemyKilled
import com.idlerpg.game.domain.event.QuestCompleted
import com.idlerpg.game.domain.system.progression.AffinityMasterySystem
import com.idlerpg.game.domain.system.quest.QuestSystem
import com.idlerpg.game.domain.system.quest.QuestClaimSystem

/** Boundaries that matter after unattended play: progress, reward banking, save and claims. */
object RecurringProgressionScenarioTest {
    fun run() {
        val factory = SimulationTestSupport.factory()
        val context = factory.createEngineContext()
        val initial = SimulationTestSupport.runtime().state()
        val affinity = Affinity.TEMPO.id
        fun mastery(xp: Long) = initial.copy(run = initial.run.copy(progression = initial.run.progression.copy(
            affinityMastery = initial.run.progression.affinityMastery.copy(experienceByAffinityId = mapOf(affinity to GameNumber.of(xp)))
        )))
        check(AffinityMasterySystem.experienceToNextLevel(mastery(0), affinity, factory.contentRegistry) == GameNumber.of(20))
        check(AffinityMasterySystem.experienceToNextLevel(mastery(19), affinity, factory.contentRegistry) == GameNumber.ONE)
        check(AffinityMasterySystem.levelFor(mastery(20), affinity, factory.contentRegistry) == 2L)
        check(AffinityMasterySystem.experienceToNextLevel(mastery(20), affinity, factory.contentRegistry) == GameNumber.of(30))
        check(AffinityMasterySystem.experienceToNextLevel(mastery(10_000), affinity, factory.contentRegistry) == GameNumber.ZERO)

        val id = TrainingHollowQuestContent.questId("hollow_patrol")
        val definition = factory.contentRegistry.quest(id)
        var state = initial
        var completions = 0
        // 62 kills = two complete 25-kill cycles plus 12 kills toward the third.
        repeat(62) { index ->
            val transition = QuestSystem.react(state, EnemyKilled(InstanceId(index.toLong() + 1L), DefaultGameContent.SLIME_ID), context)
            state = transition.state
            completions += transition.events.filterIsInstance<QuestCompleted>().count { it.questId == id }
        }
        val progress = state.run.quests.progressFor(id)
        check(completions == 2)
        check(progress.completionCount == GameNumber.of(2))
        check(progress.claimedCount == GameNumber.ZERO)
        check(progress.progressByObjectiveId[TrainingHollowQuestContent.objectiveId("hollow_patrol")] == GameNumber.of(12))
        check(state.engine == initial.engine)

        val codec = SaveCodec()
        val saved = codec.decode(codec.encode(SaveEnvelope.create(state, SimulationTestSupport.CONTENT_VERSION, 1_000L))).gameState()
        check(saved == state)
        val claimed = QuestClaimSystem.handle(saved, ClaimQuestReward(id), context) as CommandHandlingResult.Accepted
        check(SimulationTestSupport.gold(claimed.state) == SimulationTestSupport.gold(saved) + GameNumber.of(60))
        check(claimed.state.run.quests.progressFor(id).claimedCount == GameNumber.of(2))
        check(claimed.state.run.quests.progressFor(id).progressByObjectiveId == progress.progressByObjectiveId)
        check(QuestClaimSystem.handle(claimed.state, ClaimQuestReward(id), context) is CommandHandlingResult.Rejected)
        // Bank a very large number without looping rewards or truncating to a Long.
        val large = GameNumber.parse("100000000000000000000")
        val banked = state.copy(run = state.run.copy(quests = state.run.quests.copy(progressByQuestId = state.run.quests.progressByQuestId +
            (id to progress.copy(completionCount = large)))))
        val reward = QuestClaimSystem.rewardForClaim(banked, definition)
        check(reward.currencies.values.single() == GameNumber.fromBigInteger(large.toBigInteger().multiply(java.math.BigInteger.valueOf(30))))
    }
}
