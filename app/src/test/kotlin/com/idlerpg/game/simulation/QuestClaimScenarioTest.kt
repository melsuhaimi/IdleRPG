package com.idlerpg.game.simulation

import com.idlerpg.game.core.number.GameNumber
import com.idlerpg.game.core.time.GameDuration
import com.idlerpg.game.data.content.DefaultGameContent
import com.idlerpg.game.domain.command.ClaimQuestReward
import com.idlerpg.game.domain.command.CommandRejectionCode
import com.idlerpg.game.domain.engine.CommandResult
import com.idlerpg.game.domain.event.CurrencyGranted
import com.idlerpg.game.domain.event.QuestCompleted
import com.idlerpg.game.domain.event.QuestRewardClaimed

object QuestClaimScenarioTest {
    fun run() {
        val runtime = SimulationTestSupport.runtime(seed = 801L)
        val early = runtime.dispatch(ClaimQuestReward(DefaultGameContent.FIRST_HUNT_QUEST_ID))
        check((early.commandResult as CommandResult.Rejected).reason.code == CommandRejectionCode.NOT_READY)
        SimulationTestSupport.startTraining(runtime)
        val completion = runtime.advance(GameDuration.ofSeconds(140L))
        check(completion.events.any { it.event is QuestCompleted })
        val ready = runtime.state().run.quests.progressFor(DefaultGameContent.FIRST_HUNT_QUEST_ID)
        check(ready.completionCount == GameNumber.ONE && ready.claimedCount == GameNumber.ZERO)
        val beforeClaimGold = SimulationTestSupport.gold(runtime.state())
        check(beforeClaimGold > GameNumber.ZERO)
        val claim = runtime.dispatch(ClaimQuestReward(DefaultGameContent.FIRST_HUNT_QUEST_ID))
        SimulationTestSupport.checkAccepted(claim)
        check(claim.events.first().event is CurrencyGranted)
        check(claim.events.last().event is QuestRewardClaimed)
        check(SimulationTestSupport.gold(runtime.state()) == beforeClaimGold + GameNumber.of(25L))
        val before = runtime.state()
        val duplicate = runtime.dispatch(ClaimQuestReward(DefaultGameContent.FIRST_HUNT_QUEST_ID))
        check((duplicate.commandResult as CommandResult.Rejected).reason.code == CommandRejectionCode.ALREADY_CLAIMED)
        check(duplicate.events.isEmpty() && runtime.state() == before)
    }
}
