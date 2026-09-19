package com.idlerpg.game.simulation

import com.idlerpg.game.core.number.GameNumber
import com.idlerpg.game.data.content.DefaultGameContent
import com.idlerpg.game.data.local.SaveData
import com.idlerpg.game.domain.command.ApplyDoctrinePreset
import com.idlerpg.game.domain.command.DoctrinePreset
import com.idlerpg.game.domain.definition.enemy.EnemyRole
import com.idlerpg.game.domain.model.doctrine.DoctrineComparison
import com.idlerpg.game.domain.model.doctrine.DoctrineCondition
import com.idlerpg.game.domain.model.doctrine.DoctrinePredicate

object DoctrineStrategyScenarioTest {
    fun run() {
        DoctrinePreset.values().forEach { preset ->
            val runtime = SimulationTestSupport.runtime(701L + preset.ordinal)
            runtime.replaceLoadedState(
                runtime.state().copy(
                    run = runtime.state().run.copy(
                        player = runtime.state().run.player.copy(
                            equippedSkillIds = listOf(
                                DefaultGameContent.HEAVY_STRIKE_ID,
                                DefaultGameContent.QUICK_SLASH_ID,
                                DefaultGameContent.GUARD_MEND_ID
                            )
                        )
                    )
                )
            )
            val before = runtime.state().engine.nextInstanceIdCounter
            SimulationTestSupport.checkAccepted(runtime.dispatch(ApplyDoctrinePreset(preset)))
            check(runtime.state().run.doctrine.rules.size == 3)
            check(runtime.state().run.doctrine.rules.map { it.instanceId }.toSet().size == 3)
            check(runtime.state().engine.nextInstanceIdCounter == before + 3L)
        }
        val runtime = SimulationTestSupport.runtime(710L)
        SimulationTestSupport.checkAccepted(runtime.dispatch(ApplyDoctrinePreset(DoctrinePreset.BALANCED)))
        val extra = runtime.state().run.doctrine.rules.first().copy(condition = DoctrineCondition.All(listOf(
            DoctrineCondition.Predicate(DoctrinePredicate.EnemyCount(DoctrineComparison.GREATER_THAN_OR_EQUAL, GameNumber.of(3L))),
            DoctrineCondition.Predicate(DoctrinePredicate.EnemyRolePresent(EnemyRole.PROTECTOR)),
            DoctrineCondition.Predicate(DoctrinePredicate.BossPresent))))
        val state = runtime.state().copy(run = runtime.state().run.copy(doctrine = runtime.state().run.doctrine.copy(
            rules = listOf(extra))))
        check(SaveData.fromGameState(state).toGameState() == state)
    }
}
