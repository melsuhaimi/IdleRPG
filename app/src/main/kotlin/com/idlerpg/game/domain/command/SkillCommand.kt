package com.idlerpg.game.domain.command

import com.idlerpg.game.core.id.ContentId

/**
 * Player-owned manual skill input and loadout configuration.
 *
 * QueueSkillCast requests priority at the next normal PLAYER_DECISION; it never inserts
 * an out-of-band combat action. Loadout commands mutate only the ordered stable-ID list
 * already owned by PlayerState.
 */
sealed interface SkillCommand : GameCommand

data class QueueSkillCast(
    val skillId: ContentId,
    override val correlationId: CommandCorrelationId? = null
) : SkillCommand

data class ClearQueuedSkillCast(
    override val correlationId: CommandCorrelationId? = null
) : SkillCommand

data class EquipSkill(
    val skillId: ContentId,
    override val correlationId: CommandCorrelationId? = null
) : SkillCommand

data class UnequipSkill(
    val skillId: ContentId,
    override val correlationId: CommandCorrelationId? = null
) : SkillCommand

data class MoveEquippedSkill(
    val fromIndex: Int,
    val toIndex: Int,
    override val correlationId: CommandCorrelationId? = null
) : SkillCommand

data class SelectSkillEvolution(
    val skillId: ContentId,
    val evolutionId: ContentId,
    override val correlationId: CommandCorrelationId? = null
) : SkillCommand
