package com.idlerpg.game.simulation

import com.idlerpg.game.core.number.GameNumber
import com.idlerpg.game.core.time.GameDuration
import com.idlerpg.game.data.content.DefaultGameContent
import com.idlerpg.game.domain.command.AddDoctrineRule
import com.idlerpg.game.domain.definition.Affinity
import com.idlerpg.game.domain.model.doctrine.DoctrineAction
import com.idlerpg.game.domain.model.doctrine.DoctrineCondition
import com.idlerpg.game.domain.model.doctrine.DoctrinePredicate

/** Repeated effective Ember contribution must only affect future enemy generation. */
object AdaptationScenarioTest {
    fun run() {
        val factory = SimulationTestSupport.factory()
        val runtime = com.idlerpg.game.application.GameRuntime(
            initialSession = factory.newGame(77L),
            sessionFactory = factory
        )
        SimulationTestSupport.startTraining(runtime)
        runtime.replaceLoadedState(
            runtime.state().copy(
                run = runtime.state().run.copy(
                    player = runtime.state().run.player.copy(
                        equippedSkillIds = listOf(DefaultGameContent.FLAME_BRAND_ID)
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
                        DoctrinePredicate.SkillReady(
                            DefaultGameContent.FLAME_BRAND_ID
                        )
                    ),
                    action = DoctrineAction.UseSkill(
                        DefaultGameContent.FLAME_BRAND_ID
                    )
                )
            )
        )

        runtime.advance(GameDuration.ofSeconds(63L))

        val regional = runtime.state().run.adaptation.regionStateById[
            DefaultGameContent.TRAINING_HOLLOW_REGION_ID
        ] ?: error("Training Hollow adaptation state missing")

        check(
            regional.tierByAffinityId[Affinity.EMBER.id] == 2
        )
        check(
            regional.exposureByAffinityId[Affinity.EMBER.id]
                ?.pressure == GameNumber.of(320L)
        )

        val enemy = runtime.state().run.combat.enemies
            .minByOrNull { it.instanceId }
            ?: error("Future adapted enemy should be active")
        check(enemy.definitionId == DefaultGameContent.SLIME_ID)
        check(
            enemy.activeMutations.any {
                it.mutationId == DefaultGameContent.ASH_SKIN_MUTATION_ID
            }
        )
    }
}
