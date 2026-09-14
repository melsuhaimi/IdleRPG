package com.idlerpg.game.simulation

import com.idlerpg.game.core.number.GameNumber
import com.idlerpg.game.data.content.DefaultGameContent
import com.idlerpg.game.domain.command.ClaimAchievementReward
import com.idlerpg.game.domain.command.CommandRejectionCode
import com.idlerpg.game.domain.engine.CommandResult
import com.idlerpg.game.domain.event.AchievementRewardClaimed
import com.idlerpg.game.domain.event.CurrencyGranted

object AchievementClaimScenarioTest {
    fun run() {
        val runtime = SimulationTestSupport.runtime(seed = 802L)
        val early = runtime.dispatch(ClaimAchievementReward(DefaultGameContent.FORGED_FLAME_ACHIEVEMENT_ID))
        check((early.commandResult as CommandResult.Rejected).reason.code == CommandRejectionCode.NOT_READY)
        val progress = runtime.state().meta.achievements.progressFor(DefaultGameContent.FORGED_FLAME_ACHIEVEMENT_ID)
        runtime.replaceLoadedState(runtime.state().copy(meta = runtime.state().meta.copy(achievements = runtime.state().meta.achievements.copy(
            progressByAchievementId = runtime.state().meta.achievements.progressByAchievementId +
                (DefaultGameContent.FORGED_FLAME_ACHIEVEMENT_ID to progress.copy(completed = true, rewardClaimed = false))
        ))))
        val beforeGold = SimulationTestSupport.gold(runtime.state())
        val claim = runtime.dispatch(ClaimAchievementReward(DefaultGameContent.FORGED_FLAME_ACHIEVEMENT_ID))
        SimulationTestSupport.checkAccepted(claim)
        check(claim.events.first().event is CurrencyGranted)
        check(claim.events.last().event is AchievementRewardClaimed)
        check(SimulationTestSupport.gold(runtime.state()) == beforeGold + GameNumber.of(10L))
        check(runtime.state().meta.achievements.progressFor(DefaultGameContent.FORGED_FLAME_ACHIEVEMENT_ID).rewardClaimed)
        val before = runtime.state()
        val duplicate = runtime.dispatch(ClaimAchievementReward(DefaultGameContent.FORGED_FLAME_ACHIEVEMENT_ID))
        check((duplicate.commandResult as CommandResult.Rejected).reason.code == CommandRejectionCode.ALREADY_CLAIMED)
        check(duplicate.events.isEmpty() && runtime.state() == before)
    }
}
