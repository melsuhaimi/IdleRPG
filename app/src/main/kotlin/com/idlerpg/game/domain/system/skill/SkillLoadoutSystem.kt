package com.idlerpg.game.domain.system.skill

import com.idlerpg.game.domain.command.CommandRejectionCode
import com.idlerpg.game.domain.command.CommandRejectionReason
import com.idlerpg.game.domain.command.EquipSkill
import com.idlerpg.game.domain.command.MoveEquippedSkill
import com.idlerpg.game.domain.command.UnequipSkill
import com.idlerpg.game.domain.engine.CommandHandlingResult
import com.idlerpg.game.domain.engine.EngineContext
import com.idlerpg.game.domain.event.GameEvent
import com.idlerpg.game.domain.event.SkillCastQueueCleared
import com.idlerpg.game.domain.event.SkillEquipped
import com.idlerpg.game.domain.event.SkillLoadoutMoved
import com.idlerpg.game.domain.event.SkillQueueClearReason
import com.idlerpg.game.domain.event.SkillUnequipped
import com.idlerpg.game.domain.model.GameState
import com.idlerpg.game.domain.model.combat.QueuedPlayerAction

/** Deterministic owner of the player's ordered equipped-skill loadout. */
object SkillLoadoutSystem {

    fun handle(
        state: GameState,
        command: EquipSkill,
        context: EngineContext
    ): CommandHandlingResult {
        val definition = context.contentRegistry.skillOrNull(command.skillId)
            ?: return CommandHandlingResult.Rejected(
                CommandRejectionReason(
                    code = CommandRejectionCode.UNKNOWN_CONTENT,
                    subjectContentId = command.skillId
                )
            )

        val unlockRejection = SkillValidationSystem.unlockRejectionReason(
            state = state,
            definition = definition
        )
        if (unlockRejection != null) {
            return CommandHandlingResult.Rejected(unlockRejection)
        }

        val equipped = state.run.player.equippedSkillIds
        if (command.skillId in equipped) {
            return CommandHandlingResult.Rejected(
                CommandRejectionReason(
                    code = CommandRejectionCode.ALREADY_OWNED,
                    subjectContentId = command.skillId
                )
            )
        }
        if (equipped.size >= context.balanceConfig.skillLoadoutCapacity) {
            return CommandHandlingResult.Rejected(
                CommandRejectionReason(
                    code = CommandRejectionCode.CAPACITY_EXCEEDED,
                    subjectContentId = command.skillId
                )
            )
        }

        val next = equipped + command.skillId
        return CommandHandlingResult.Accepted(
            state = state.copy(
                run = state.run.copy(
                    player = state.run.player.copy(
                        equippedSkillIds = next
                    )
                )
            ),
            events = listOf(
                SkillEquipped(
                    skillId = command.skillId,
                    index = next.lastIndex
                )
            )
        )
    }

    fun handle(
        state: GameState,
        command: UnequipSkill,
        context: EngineContext
    ): CommandHandlingResult {
        val equipped = state.run.player.equippedSkillIds
        if (command.skillId !in equipped) {
            return CommandHandlingResult.Rejected(
                CommandRejectionReason(
                    code = CommandRejectionCode.NOT_OWNED,
                    subjectContentId = command.skillId
                )
            )
        }

        val queued = state.run.combat.queuedPlayerAction
        val clearsQueue = queued is QueuedPlayerAction.Skill && queued.skillId == command.skillId
        val events = mutableListOf<GameEvent>(SkillUnequipped(command.skillId))
        if (clearsQueue) {
            events += SkillCastQueueCleared(
                skillId = command.skillId,
                reason = SkillQueueClearReason.UNEQUIPPED
            )
        }

        return CommandHandlingResult.Accepted(
            state = state.copy(
                run = state.run.copy(
                    player = state.run.player.copy(
                        equippedSkillIds = equipped.filterNot { it == command.skillId }
                    ),
                    combat =
                        if (clearsQueue) {
                            state.run.combat.copy(queuedPlayerAction = null)
                        } else {
                            state.run.combat
                        }
                )
            ),
            events = events
        )
    }

    fun handle(
        state: GameState,
        command: MoveEquippedSkill,
        context: EngineContext
    ): CommandHandlingResult {
        val equipped = state.run.player.equippedSkillIds
        if (command.fromIndex !in equipped.indices || command.toIndex !in equipped.indices) {
            return CommandHandlingResult.Rejected(
                CommandRejectionReason(
                    code = CommandRejectionCode.INVALID_ARGUMENT
                )
            )
        }
        if (command.fromIndex == command.toIndex) {
            return CommandHandlingResult.Accepted(state = state)
        }

        val mutable = equipped.toMutableList()
        val skillId = mutable.removeAt(command.fromIndex)
        mutable.add(command.toIndex, skillId)

        return CommandHandlingResult.Accepted(
            state = state.copy(
                run = state.run.copy(
                    player = state.run.player.copy(
                        equippedSkillIds = mutable.toList()
                    )
                )
            ),
            events = listOf(
                SkillLoadoutMoved(
                    skillId = skillId,
                    fromIndex = command.fromIndex,
                    toIndex = command.toIndex
                )
            )
        )
    }
}
