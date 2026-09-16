package com.idlerpg.game.simulation

import com.idlerpg.game.application.GameRuntime
import com.idlerpg.game.core.number.GameNumber
import com.idlerpg.game.data.content.DefaultGameContent
import com.idlerpg.game.domain.command.RefineSkill
import com.idlerpg.game.domain.command.UpgradeSkillMastery
import com.idlerpg.game.domain.command.UpgradeSkillRank
import com.idlerpg.game.domain.definition.CurrencyId
import com.idlerpg.game.domain.engine.CommandResult
import com.idlerpg.game.domain.event.CurrencySpent
import com.idlerpg.game.domain.event.SkillMasteryIncreased
import com.idlerpg.game.domain.event.SkillRankIncreased
import com.idlerpg.game.domain.event.SkillRefinementIncreased
import com.idlerpg.game.domain.system.skill.SkillProgressionSystem
import com.idlerpg.game.domain.system.skill.SkillScalingSystem

/** Explicit skill investment is bounded, charged atomically, and affects skill scaling. */
object SkillProgressionScenarioTest {
    fun run() {
        val factory = SimulationTestSupport.factory()
        val runtime = GameRuntime(
            initialSession = factory.newGame(919L),
            sessionFactory = factory
        )
        val initial = runtime.state()
        runtime.replaceLoadedState(
            initial.copy(
                run = initial.run.copy(
                    economy = initial.run.economy.copy(
                        wallet = initial.run.economy.wallet.copy(
                            amountsByCurrencyId = mapOf(
                                CurrencyId.GOLD to GameNumber.of(1_000_000_000L)
                            )
                        )
                    )
                )
            )
        )

        val skillId = DefaultGameContent.HEAVY_STRIKE_ID
        val highLevel = runtime.state().copy(
            run = runtime.state().run.copy(
                progression = runtime.state().run.progression.copy(
                    playerLevel = com.idlerpg.game.domain.model.progression.PlayerLevelState(
                        level = 1_000L
                    )
                )
            )
        )
        check(
            SkillScalingSystem.rank(
                highLevel,
                factory.contentRegistry.skill(skillId)
            ) == 1L
        )
        val rankBefore = SkillScalingSystem.rank(
            runtime.state(),
            factory.contentRegistry.skill(skillId)
        )
        val rankCost = SkillProgressionSystem.rankUpgradeCost(rankBefore)
        val rank = runtime.dispatch(UpgradeSkillRank(skillId))
        check(rank.commandResult == CommandResult.Accepted)
        check(rank.events.any { it.event is CurrencySpent })
        check(rank.events.any { it.event is SkillRankIncreased })
        check(
            SkillScalingSystem.rank(runtime.state(), factory.contentRegistry.skill(skillId)) ==
                rankBefore + 1L
        )
        check(
            SimulationTestSupport.gold(runtime.state()) ==
                GameNumber.of(1_000_000_000L) - rankCost
        )

        val masteryBefore = SkillScalingSystem.mastery(
            runtime.state(),
            factory.contentRegistry.skill(skillId)
        )
        val mastery = runtime.dispatch(UpgradeSkillMastery(skillId))
        check(mastery.commandResult == CommandResult.Accepted)
        check(mastery.events.any { it.event is SkillMasteryIncreased })
        check(
            SkillScalingSystem.mastery(runtime.state(), factory.contentRegistry.skill(skillId)) ==
                masteryBefore + 1L
        )

        val refinementBefore = SkillScalingSystem.refinement(
            runtime.state(),
            factory.contentRegistry.skill(skillId)
        )
        val refinement = runtime.dispatch(RefineSkill(skillId))
        check(refinement.commandResult == CommandResult.Accepted)
        check(refinement.events.any { it.event is SkillRefinementIncreased })
        check(
            SkillScalingSystem.refinement(runtime.state(), factory.contentRegistry.skill(skillId)) ==
                refinementBefore + 1L
        )

        val beforeRejected = runtime.state()
        runtime.replaceLoadedState(
            beforeRejected.copy(
                run = beforeRejected.run.copy(
                    economy = beforeRejected.run.economy.copy(
                        wallet = beforeRejected.run.economy.wallet.copy(
                            amountsByCurrencyId = emptyMap()
                        )
                    )
                )
            )
        )
        val rejected = runtime.dispatch(UpgradeSkillMastery(skillId))
        check((rejected.commandResult as CommandResult.Rejected).reason.code ==
            com.idlerpg.game.domain.command.CommandRejectionCode.INSUFFICIENT_RESOURCE)
        check(rejected.state.run.progression.skillProgression ==
            beforeRejected.run.progression.skillProgression)
    }
}
