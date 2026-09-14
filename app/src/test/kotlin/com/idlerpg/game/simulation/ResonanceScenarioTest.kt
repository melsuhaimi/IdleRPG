package com.idlerpg.game.simulation

import com.idlerpg.game.core.number.GameNumber
import com.idlerpg.game.core.number.Ratio
import com.idlerpg.game.core.time.GameDuration
import com.idlerpg.game.data.content.DefaultGameContent
import com.idlerpg.game.domain.command.AddDoctrineRule
import com.idlerpg.game.domain.definition.Affinity
import com.idlerpg.game.domain.event.ConvergenceTriggered
import com.idlerpg.game.domain.model.doctrine.DoctrineAction
import com.idlerpg.game.domain.model.doctrine.DoctrineComparison
import com.idlerpg.game.domain.model.doctrine.DoctrineCondition
import com.idlerpg.game.domain.model.doctrine.DoctrinePredicate

/** Ordered Might, Might, Ember emission must trigger Forged Flame deterministically. */
object ResonanceScenarioTest {
    fun run() {
        val factory = SimulationTestSupport.factory()
        val runtime = com.idlerpg.game.application.GameRuntime(
            initialSession = factory.newGame(33L),
            sessionFactory = factory
        )
        SimulationTestSupport.startTraining(runtime)

        val configured = runtime.state().copy(
            run = runtime.state().run.copy(
                player = runtime.state().run.player.copy(
                    equippedSkillIds = listOf(
                        DefaultGameContent.HEAVY_STRIKE_ID,
                        DefaultGameContent.FLAME_BRAND_ID
                    )
                ),
                progression = runtime.state().run.progression.copy(
                    featureUnlocks =
                        runtime.state().run.progression.featureUnlocks.copy(
                            unlockedFeatureIds =
                                runtime.state().run.progression.featureUnlocks
                                    .unlockedFeatureIds +
                                    DefaultGameContent.FLAME_BRAND_FEATURE_ID
                        )
                )
            )
        )
        runtime.replaceLoadedState(configured)

        SimulationTestSupport.checkAccepted(
            runtime.dispatch(
                AddDoctrineRule(
                    condition = DoctrineCondition.Predicate(
                        DoctrinePredicate.ResonanceCharge(
                            affinity = Affinity.MIGHT,
                            comparison = DoctrineComparison.GREATER_THAN_OR_EQUAL,
                            amount = GameNumber.of(2L)
                        )
                    ),
                    action = DoctrineAction.UseSkill(
                        DefaultGameContent.FLAME_BRAND_ID
                    )
                )
            )
        )
        SimulationTestSupport.checkAccepted(
            runtime.dispatch(
                AddDoctrineRule(
                    condition = DoctrineCondition.Predicate(
                        DoctrinePredicate.EnemyHealthPercent(
                            comparison = DoctrineComparison.LESS_THAN_OR_EQUAL,
                            threshold = Ratio.ONE
                        )
                    ),
                    action = DoctrineAction.UseSkill(
                        DefaultGameContent.HEAVY_STRIKE_ID
                    )
                )
            )
        )

        val result = runtime.advance(GameDuration.ofSeconds(4L))
        val triggers = result.events.count {
            val event = it.event
            event is ConvergenceTriggered &&
                event.convergenceId == DefaultGameContent.FORGED_FLAME_ID
        }
        check(triggers == 1)
        check(
            DefaultGameContent.FORGED_FLAME_ID in
                runtime.state().meta.discoveries.discoveredConvergenceIds
        )
    }
}
