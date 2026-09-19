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
import com.idlerpg.game.domain.model.world.RegionProgressState
import com.idlerpg.game.domain.model.world.WorldAutomationMode

object QuestClaimScenarioTest {
    fun run() {
        val runtime = SimulationTestSupport.runtime(seed = 801L)
        runtime.replaceLoadedState(
            runtime.state().copy(
                run = runtime.state().run.copy(
                    world = runtime.state().run.world.copy(
                        activeRegionId = DefaultGameContent.TRAINING_HOLLOW_REGION_ID,
                        unlockedRegionIds = setOf(DefaultGameContent.TRAINING_HOLLOW_REGION_ID),
                        regionProgressById = mapOf(
                            DefaultGameContent.TRAINING_HOLLOW_REGION_ID to
                                RegionProgressState(
                                    highestClearedEncounterTier = 1L,
                                    normalClears = GameNumber.ONE
                                )
                        ),
                        automationMode = WorldAutomationMode.FARM,
                        selectedFarmEncounterId = DefaultGameContent.TRAINING_SLIME_ENCOUNTER_ID,
                        clearedEncounterIds = setOf(
                            DefaultGameContent.TRAINING_SLIME_ENCOUNTER_ID
                        )
                    )
                )
            )
        )
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
