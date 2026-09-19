package com.idlerpg.game.simulation

import com.idlerpg.game.core.number.GameNumber
import com.idlerpg.game.core.number.Ratio
import com.idlerpg.game.core.time.GameDuration
import com.idlerpg.game.data.content.DefaultGameContent
import com.idlerpg.game.domain.command.AddDoctrineRule
import com.idlerpg.game.domain.definition.Affinity
import com.idlerpg.game.domain.event.SkillUsed
import com.idlerpg.game.domain.model.doctrine.DoctrineAction
import com.idlerpg.game.domain.model.doctrine.DoctrineComparison
import com.idlerpg.game.domain.model.doctrine.DoctrineCondition
import com.idlerpg.game.domain.model.doctrine.DoctrinePredicate

/** Ordered Doctrine rules must autonomously reproduce a stable skill sequence. */
object DoctrineScenarioTest {
    fun run() {
        fun build(): com.idlerpg.game.application.GameRuntime {
            val factory = SimulationTestSupport.factory()
            val runtime = com.idlerpg.game.application.GameRuntime(
                initialSession = factory.newGame(51L),
                sessionFactory = factory
            )
            SimulationTestSupport.startTraining(runtime)
            runtime.replaceLoadedState(
                runtime.state().copy(
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
            )
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
            return runtime
        }

        val first = build()
        val second = build()
        // Heavy Strike's cooldown deliberately inserts basic-attack decision slots between
        // the authored skill uses; observe a full two-pattern window.
        val firstEvents = first.advance(GameDuration.ofSeconds(12L)).events
        val secondEvents = second.advance(GameDuration.ofSeconds(12L)).events

        val firstSkills = firstEvents.mapNotNull { (it.event as? SkillUsed)?.skillId }
        val secondSkills = secondEvents.mapNotNull { (it.event as? SkillUsed)?.skillId }

        check(firstSkills == secondSkills)
        check(
            firstSkills.take(6) == listOf(
                DefaultGameContent.HEAVY_STRIKE_ID,
                DefaultGameContent.HEAVY_STRIKE_ID,
                DefaultGameContent.FLAME_BRAND_ID,
                DefaultGameContent.HEAVY_STRIKE_ID,
                DefaultGameContent.HEAVY_STRIKE_ID,
                DefaultGameContent.FLAME_BRAND_ID
            )
        )
    }
}
