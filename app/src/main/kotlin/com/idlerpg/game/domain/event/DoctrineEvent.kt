package com.idlerpg.game.domain.event

import com.idlerpg.game.core.id.ContentId
import com.idlerpg.game.core.id.InstanceId
import com.idlerpg.game.domain.command.CommandRejectionReason

/** Completed Doctrine-automation facts. */
sealed interface DoctrineEvent : GameEvent

data class DoctrineUpdated(
    val enabled: Boolean,
    val orderedRuleIds: List<InstanceId> = emptyList()
) : DoctrineEvent {
    init {
        require(orderedRuleIds.size == orderedRuleIds.toSet().size) {
            "DoctrineUpdated.orderedRuleIds cannot contain duplicates"
        }
    }
}

data class DoctrineRuleSelected(
    val ruleId: InstanceId,
    val actionId: ContentId,
    val explanation: String
) : DoctrineEvent
data class DoctrineRuleEvaluated(val ruleId: InstanceId, val matched: Boolean, val explanation: String) : DoctrineEvent

data class DoctrineActionRejected(
    val ruleId: InstanceId,
    val actionId: ContentId,
    val reason: CommandRejectionReason
) : DoctrineEvent

data class DoctrineFallbackUsed(
    val actionId: ContentId,
    val explanation: String
) : DoctrineEvent
