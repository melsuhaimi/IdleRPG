package com.idlerpg.game.domain.event

import com.idlerpg.game.core.id.ContentId
import com.idlerpg.game.domain.command.CommandRejectionCode

/** Completed facts for manual skill queueing and ordered skill-loadout changes. */
sealed interface SkillEvent : GameEvent

enum class SkillQueueClearReason {
    PLAYER_REQUEST,
    UNEQUIPPED,
    INVALIDATED,
    COMBAT_ENDED
}

data class SkillCastQueued(
    val skillId: ContentId
) : SkillEvent

data class SkillCastQueueReplaced(
    val previousSkillId: ContentId,
    val newSkillId: ContentId
) : SkillEvent

data class SkillCastQueueDeferred(
    val skillId: ContentId,
    val reasonCode: CommandRejectionCode
) : SkillEvent {
    init {
        require(
            reasonCode == CommandRejectionCode.COOLDOWN_ACTIVE ||
                reasonCode == CommandRejectionCode.INSUFFICIENT_RESOURCE
        ) {
            "SkillCastQueueDeferred requires a transient execution rejection: $reasonCode"
        }
    }
}

data class SkillCastQueueConsumed(
    val skillId: ContentId
) : SkillEvent

data class SkillCastQueueCleared(
    val skillId: ContentId,
    val reason: SkillQueueClearReason
) : SkillEvent

data class SkillEquipped(
    val skillId: ContentId,
    val index: Int
) : SkillEvent {
    init {
        require(index >= 0) { "SkillEquipped.index cannot be negative: $index" }
    }
}

data class SkillUnequipped(
    val skillId: ContentId
) : SkillEvent

data class SkillLoadoutMoved(
    val skillId: ContentId,
    val fromIndex: Int,
    val toIndex: Int
) : SkillEvent {
    init {
        require(fromIndex >= 0) { "fromIndex cannot be negative: $fromIndex" }
        require(toIndex >= 0) { "toIndex cannot be negative: $toIndex" }
    }
}

data class SkillEvolutionSelected(
    val skillId: ContentId,
    val evolutionId: ContentId,
    val previousEvolutionId: ContentId?
) : SkillEvent
