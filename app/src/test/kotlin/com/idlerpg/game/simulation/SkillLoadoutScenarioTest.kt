package com.idlerpg.game.simulation

import com.idlerpg.game.core.number.Ratio
import com.idlerpg.game.core.time.GameDuration
import com.idlerpg.game.data.content.DefaultGameContent
import com.idlerpg.game.data.local.SaveData
import com.idlerpg.game.domain.command.AddDoctrineRule
import com.idlerpg.game.domain.command.CommandRejectionCode
import com.idlerpg.game.domain.command.EquipSkill
import com.idlerpg.game.domain.command.MoveEquippedSkill
import com.idlerpg.game.domain.command.QueueSkillCast
import com.idlerpg.game.domain.command.UnequipSkill
import com.idlerpg.game.domain.engine.CommandResult
import com.idlerpg.game.domain.engine.EngineResult
import com.idlerpg.game.domain.event.ActionSelected
import com.idlerpg.game.domain.event.SkillCastQueueCleared
import com.idlerpg.game.domain.event.SkillEquipped
import com.idlerpg.game.domain.event.SkillLoadoutMoved
import com.idlerpg.game.domain.event.SkillQueueClearReason
import com.idlerpg.game.domain.event.SkillUnequipped
import com.idlerpg.game.domain.event.SkillUsed
import com.idlerpg.game.domain.model.doctrine.DoctrineAction
import com.idlerpg.game.domain.model.doctrine.DoctrineComparison
import com.idlerpg.game.domain.model.doctrine.DoctrineCondition
import com.idlerpg.game.domain.model.doctrine.DoctrinePredicate

/** FBE-01 skill-loadout capacity, ordering, unlock, persistence, and Doctrine regression. */
object SkillLoadoutScenarioTest {

    fun run() {
        loadoutCommandsAndPersistence()
        unequipClearsQueuedSkill()
        doctrineCannotUseUnequippedSkill()
    }

    private fun loadoutCommandsAndPersistence() {
        val runtime = SimulationTestSupport.runtime(seed = 711L)

        val heavy = runtime.dispatch(EquipSkill(DefaultGameContent.HEAVY_STRIKE_ID))
        SimulationTestSupport.checkAccepted(heavy)
        check((heavy.events.single().event as SkillEquipped).index == 0)

        checkRejected(
            runtime.dispatch(EquipSkill(DefaultGameContent.HEAVY_STRIKE_ID)),
            CommandRejectionCode.ALREADY_OWNED
        )
        checkRejected(
            runtime.dispatch(EquipSkill(DefaultGameContent.CINDER_MARK_ID)),
            CommandRejectionCode.LOCKED
        )

        runtime.replaceLoadedState(
            runtime.state().copy(
                run = runtime.state().run.copy(
                    progression = runtime.state().run.progression.copy(
                        featureUnlocks = runtime.state().run.progression.featureUnlocks.copy(
                            unlockedFeatureIds =
                                runtime.state().run.progression.featureUnlocks.unlockedFeatureIds +
                                    setOf(
                                        DefaultGameContent.CINDER_MARK_FEATURE_ID,
                                        DefaultGameContent.GUARD_MEND_FEATURE_ID,
                                        DefaultGameContent.FLAME_BRAND_FEATURE_ID
                                    )
                        )
                    )
                )
            )
        )

        SimulationTestSupport.checkAccepted(
            runtime.dispatch(EquipSkill(DefaultGameContent.QUICK_SLASH_ID))
        )
        SimulationTestSupport.checkAccepted(
            runtime.dispatch(EquipSkill(DefaultGameContent.CINDER_MARK_ID))
        )
        SimulationTestSupport.checkAccepted(
            runtime.dispatch(EquipSkill(DefaultGameContent.GUARD_MEND_ID))
        )
        check(runtime.state().run.player.equippedSkillIds.size == 4)

        checkRejected(
            runtime.dispatch(EquipSkill(DefaultGameContent.FLAME_BRAND_ID)),
            CommandRejectionCode.CAPACITY_EXCEEDED
        )

        val beforeMove = runtime.state().run.player.equippedSkillIds
        val movedSkill = beforeMove.first()
        val move = runtime.dispatch(MoveEquippedSkill(fromIndex = 0, toIndex = 3))
        SimulationTestSupport.checkAccepted(move)
        val movedEvent = move.events.single().event as SkillLoadoutMoved
        check(movedEvent.skillId == movedSkill)
        check(movedEvent.fromIndex == 0)
        check(movedEvent.toIndex == 3)
        check(runtime.state().run.player.equippedSkillIds.last() == movedSkill)

        val persisted = SaveData.fromGameState(runtime.state()).toGameState()
        check(persisted.run.player.equippedSkillIds == runtime.state().run.player.equippedSkillIds)

        checkRejected(
            runtime.dispatch(MoveEquippedSkill(fromIndex = -1, toIndex = 0)),
            CommandRejectionCode.INVALID_ARGUMENT
        )
        checkRejected(
            runtime.dispatch(MoveEquippedSkill(fromIndex = 0, toIndex = 99)),
            CommandRejectionCode.INVALID_ARGUMENT
        )
    }

    private fun unequipClearsQueuedSkill() {
        val runtime = SimulationTestSupport.runtime(seed = 712L)
        SimulationTestSupport.checkAccepted(
            runtime.dispatch(EquipSkill(DefaultGameContent.QUICK_SLASH_ID))
        )
        SimulationTestSupport.startTraining(runtime)
        SimulationTestSupport.checkAccepted(
            runtime.dispatch(QueueSkillCast(DefaultGameContent.QUICK_SLASH_ID))
        )

        val result = runtime.dispatch(UnequipSkill(DefaultGameContent.QUICK_SLASH_ID))
        SimulationTestSupport.checkAccepted(result)
        check(result.events.any { it.event is SkillUnequipped })
        val clear = result.events
            .map { it.event }
            .filterIsInstance<SkillCastQueueCleared>()
            .single()
        check(clear.reason == SkillQueueClearReason.UNEQUIPPED)
        check(runtime.state().run.combat.queuedPlayerAction == null)
        check(DefaultGameContent.QUICK_SLASH_ID !in runtime.state().run.player.equippedSkillIds)
    }

    private fun doctrineCannotUseUnequippedSkill() {
        val runtime = SimulationTestSupport.runtime(seed = 713L)
        SimulationTestSupport.checkAccepted(
            runtime.dispatch(EquipSkill(DefaultGameContent.HEAVY_STRIKE_ID))
        )
        SimulationTestSupport.startTraining(runtime)
        SimulationTestSupport.checkAccepted(
            runtime.dispatch(
                AddDoctrineRule(
                    condition = DoctrineCondition.Predicate(
                        DoctrinePredicate.EnemyHealthPercent(
                            comparison = DoctrineComparison.LESS_THAN_OR_EQUAL,
                            threshold = Ratio.ONE
                        )
                    ),
                    action = DoctrineAction.UseSkill(DefaultGameContent.HEAVY_STRIKE_ID)
                )
            )
        )
        SimulationTestSupport.checkAccepted(
            runtime.dispatch(UnequipSkill(DefaultGameContent.HEAVY_STRIKE_ID))
        )

        val result = runtime.advance(GameDuration.ofSeconds(1L))
        val playerId = result.state.run.combat.playerCombatant?.instanceId
            ?: error("Expected active player combatant")
        val selected = result.events.mapNotNull { it.event as? ActionSelected }
            .single { it.actorInstanceId == playerId }
        check(selected.actionId == DefaultGameContent.BASIC_ATTACK_ID)
        check(
            result.events.none {
                (it.event as? SkillUsed)?.skillId == DefaultGameContent.HEAVY_STRIKE_ID
            }
        )
    }

    private fun checkRejected(
        result: EngineResult,
        expected: CommandRejectionCode
    ) {
        val rejected = result.commandResult as? CommandResult.Rejected
            ?: error("Expected rejected command, got ${result.commandResult}")
        check(rejected.reason.code == expected) {
            "Expected $expected, got ${rejected.reason}"
        }
    }
}
