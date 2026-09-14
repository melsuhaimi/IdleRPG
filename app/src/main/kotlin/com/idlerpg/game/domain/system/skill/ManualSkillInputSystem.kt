package com.idlerpg.game.domain.system.skill

import com.idlerpg.game.domain.command.ClearQueuedSkillCast
import com.idlerpg.game.domain.command.QueueSkillCast
import com.idlerpg.game.domain.engine.CommandHandlingResult
import com.idlerpg.game.domain.engine.EngineContext
import com.idlerpg.game.domain.event.SkillCastQueueCleared
import com.idlerpg.game.domain.event.SkillCastQueueReplaced
import com.idlerpg.game.domain.event.SkillCastQueued
import com.idlerpg.game.domain.event.SkillQueueClearReason
import com.idlerpg.game.domain.model.GameState
import com.idlerpg.game.domain.model.combat.QueuedPlayerAction

/**
 * Owns explicit player requests to queue or clear the next manual skill priority.
 *
 * Queue-time validation intentionally ignores cooldown/resource readiness. Those are
 * transient execution-time conditions: the queue remains pending while Doctrine handles
 * intervening decisions until the skill becomes legal.
 */
object ManualSkillInputSystem {

    fun handle(
        state: GameState,
        command: QueueSkillCast,
        context: EngineContext
    ): CommandHandlingResult {
        val rejection = SkillValidationSystem.queueRejectionReason(
            state = state,
            skillId = command.skillId,
            contentRegistry = context.contentRegistry
        )
        if (rejection != null) {
            return CommandHandlingResult.Rejected(rejection)
        }

        val previous = state.run.combat.queuedPlayerAction
        if (previous is QueuedPlayerAction.Skill && previous.skillId == command.skillId) {
            return CommandHandlingResult.Accepted(state = state)
        }

        val nextState = state.copy(
            run = state.run.copy(
                combat = state.run.combat.copy(
                    queuedPlayerAction = QueuedPlayerAction.Skill(command.skillId)
                )
            )
        )

        return CommandHandlingResult.Accepted(
            state = nextState,
            events = listOf(
                when (previous) {
                    is QueuedPlayerAction.Skill -> SkillCastQueueReplaced(
                        previousSkillId = previous.skillId,
                        newSkillId = command.skillId
                    )
                    null -> SkillCastQueued(command.skillId)
                }
            )
        )
    }

    fun handle(
        state: GameState,
        command: ClearQueuedSkillCast,
        context: EngineContext
    ): CommandHandlingResult {
        val queued = state.run.combat.queuedPlayerAction
            ?: return CommandHandlingResult.Accepted(state = state)

        val skillId = (queued as QueuedPlayerAction.Skill).skillId
        return CommandHandlingResult.Accepted(
            state = state.copy(
                run = state.run.copy(
                    combat = state.run.combat.copy(
                        queuedPlayerAction = null
                    )
                )
            ),
            events = listOf(
                SkillCastQueueCleared(
                    skillId = skillId,
                    reason = SkillQueueClearReason.PLAYER_REQUEST
                )
            )
        )
    }
}
