package com.idlerpg.game.simulation

import com.idlerpg.game.application.GameRuntime
import com.idlerpg.game.core.time.GameDuration
import com.idlerpg.game.data.content.DefaultGameContent
import com.idlerpg.game.data.local.SaveCodec
import com.idlerpg.game.data.local.SaveData
import com.idlerpg.game.data.local.SaveEnvelope
import com.idlerpg.game.data.local.SaveVersion
import com.idlerpg.game.domain.command.ClearQueuedSkillCast
import com.idlerpg.game.domain.command.EquipSkill
import com.idlerpg.game.domain.command.QueueSkillCast
import com.idlerpg.game.domain.command.RetreatEncounter
import com.idlerpg.game.domain.event.ActionSelected
import com.idlerpg.game.domain.event.SkillCastQueueCleared
import com.idlerpg.game.domain.event.SkillCastQueueConsumed
import com.idlerpg.game.domain.event.SkillCastQueueDeferred
import com.idlerpg.game.domain.event.SkillCastQueueReplaced
import com.idlerpg.game.domain.event.SkillCastQueued
import com.idlerpg.game.domain.event.SkillQueueClearReason
import com.idlerpg.game.domain.event.SkillUsed
import com.idlerpg.game.domain.model.combat.QueuedPlayerAction

/** FBE-01 deterministic manual-skill queue and single-action-stream regression. */
object ManualSkillQueueScenarioTest {

    fun run() {
        queueReplaceClear()
        consumeDeferRetryAndSaveLoad()
        retreatClearsQueue()
    }

    private fun queueReplaceClear() {
        val runtime = SimulationTestSupport.runtime(seed = 701L)
        SimulationTestSupport.checkAccepted(
            runtime.dispatch(EquipSkill(DefaultGameContent.HEAVY_STRIKE_ID))
        )
        SimulationTestSupport.checkAccepted(
            runtime.dispatch(EquipSkill(DefaultGameContent.QUICK_SLASH_ID))
        )
        SimulationTestSupport.startTraining(runtime)

        val firstQueue = runtime.dispatch(
            QueueSkillCast(DefaultGameContent.HEAVY_STRIKE_ID)
        )
        SimulationTestSupport.checkAccepted(firstQueue)
        check(firstQueue.events.single().event is SkillCastQueued)

        val replacement = runtime.dispatch(
            QueueSkillCast(DefaultGameContent.QUICK_SLASH_ID)
        )
        SimulationTestSupport.checkAccepted(replacement)
        val replaced = replacement.events.single().event as SkillCastQueueReplaced
        check(replaced.previousSkillId == DefaultGameContent.HEAVY_STRIKE_ID)
        check(replaced.newSkillId == DefaultGameContent.QUICK_SLASH_ID)
        check(
            runtime.state().run.combat.queuedPlayerAction ==
                QueuedPlayerAction.Skill(DefaultGameContent.QUICK_SLASH_ID)
        )

        val clear = runtime.dispatch(ClearQueuedSkillCast())
        SimulationTestSupport.checkAccepted(clear)
        val cleared = clear.events.single().event as SkillCastQueueCleared
        check(cleared.skillId == DefaultGameContent.QUICK_SLASH_ID)
        check(cleared.reason == SkillQueueClearReason.PLAYER_REQUEST)
        check(runtime.state().run.combat.queuedPlayerAction == null)

        val idempotentClear = runtime.dispatch(ClearQueuedSkillCast())
        SimulationTestSupport.checkAccepted(idempotentClear)
        check(idempotentClear.events.isEmpty())
    }

    private fun consumeDeferRetryAndSaveLoad() {
        val runtime = SimulationTestSupport.runtime(seed = 702L)
        SimulationTestSupport.checkAccepted(
            runtime.dispatch(EquipSkill(DefaultGameContent.HEAVY_STRIKE_ID))
        )
        SimulationTestSupport.startTraining(runtime)

        SimulationTestSupport.checkAccepted(
            runtime.dispatch(QueueSkillCast(DefaultGameContent.HEAVY_STRIKE_ID))
        )

        val firstDecision = runtime.advance(GameDuration.ofSeconds(1L))
        val firstPlayerId = firstDecision.state.run.combat.playerCombatant?.instanceId
            ?: error("Expected active player combatant")
        val firstActions = firstDecision.events.mapNotNull { it.event as? ActionSelected }
            .filter { it.actorInstanceId == firstPlayerId }
        check(firstActions.size == 1)
        check(firstActions.single().actionId == DefaultGameContent.HEAVY_STRIKE_ID)
        check(firstDecision.events.any { it.event is SkillCastQueueConsumed })
        check(
            firstDecision.events.any {
                (it.event as? SkillUsed)?.skillId == DefaultGameContent.HEAVY_STRIKE_ID
            }
        )
        check(runtime.state().run.combat.queuedPlayerAction == null)

        SimulationTestSupport.checkAccepted(
            runtime.dispatch(QueueSkillCast(DefaultGameContent.HEAVY_STRIKE_ID))
        )
        val queuedState = runtime.state()
        check(
            queuedState.run.combat.queuedPlayerAction ==
                QueuedPlayerAction.Skill(DefaultGameContent.HEAVY_STRIKE_ID)
        )

        val envelope = SaveEnvelope(
            schemaVersion = SaveVersion.CURRENT,
            contentVersion = SimulationTestSupport.CONTENT_VERSION,
            writtenAtEpochMs = 702_000L,
            data = SaveData.fromGameState(queuedState)
        )
        val codec = SaveCodec()
        val loadedState = codec.decode(codec.encode(envelope)).gameState()
        check(loadedState == queuedState)

        val factoryA = SimulationTestSupport.factory()
        val factoryB = SimulationTestSupport.factory()
        val active = GameRuntime(
            initialSession = factoryA.loadedGame(queuedState),
            sessionFactory = factoryA
        )
        val loaded = GameRuntime(
            initialSession = factoryB.loadedGame(loadedState),
            sessionFactory = factoryB
        )

        val activeResult = active.advance(GameDuration.ofSeconds(2L))
        val loadedResult = loaded.advance(GameDuration.ofSeconds(2L))
        check(activeResult == loadedResult)

        val activeRetryResult = active.advance(GameDuration.ofSeconds(5L))
        val loadedRetryResult = loaded.advance(GameDuration.ofSeconds(5L))
        check(activeRetryResult == loadedRetryResult)

        val allEvents = activeResult.events + activeRetryResult.events
        val deferred = allEvents.filter { it.event is SkillCastQueueDeferred }
        val consumed = allEvents.filter { it.event is SkillCastQueueConsumed }
        val activePlayerId = activeRetryResult.state.run.combat.playerCombatant?.instanceId
            ?: error("Expected active player combatant")
        val actions = allEvents.mapNotNull { it.event as? ActionSelected }
            .filter { it.actorInstanceId == activePlayerId }

        check(deferred.isNotEmpty())
        check(consumed.isNotEmpty())
        check(actions.any { it.actionId == DefaultGameContent.BASIC_ATTACK_ID })
        check(actions.any { it.actionId == DefaultGameContent.HEAVY_STRIKE_ID })
        check(active.state().run.combat.queuedPlayerAction == null)
    }

    private fun retreatClearsQueue() {
        val runtime = SimulationTestSupport.runtime(seed = 703L)
        SimulationTestSupport.checkAccepted(
            runtime.dispatch(EquipSkill(DefaultGameContent.QUICK_SLASH_ID))
        )
        SimulationTestSupport.startTraining(runtime)
        SimulationTestSupport.checkAccepted(
            runtime.dispatch(QueueSkillCast(DefaultGameContent.QUICK_SLASH_ID))
        )

        val retreat = runtime.dispatch(RetreatEncounter())
        SimulationTestSupport.checkAccepted(retreat)
        val clear = retreat.events
            .map { it.event }
            .filterIsInstance<SkillCastQueueCleared>()
            .single()
        check(clear.skillId == DefaultGameContent.QUICK_SLASH_ID)
        check(clear.reason == SkillQueueClearReason.COMBAT_ENDED)
        check(runtime.state().run.combat.queuedPlayerAction == null)
    }
}
