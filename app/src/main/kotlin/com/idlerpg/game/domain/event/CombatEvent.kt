package com.idlerpg.game.domain.event

import com.idlerpg.game.core.id.ContentId
import com.idlerpg.game.core.id.InstanceId
import com.idlerpg.game.core.number.GameNumber

/** Completed combat-domain facts. */
sealed interface CombatEvent : GameEvent

enum class CombatEndReason {
    VICTORY,
    DEFEAT,
    RETREATED
}

data class CombatStarted(
    val combatSequenceId: Long,
    val encounterDefinitionId: ContentId? = null
) : CombatEvent {
    init {
        require(combatSequenceId > 0L) {
            "CombatStarted.combatSequenceId must be positive: $combatSequenceId"
        }
    }
}

data class ActionSelected(
    val actorInstanceId: InstanceId,
    val actionId: ContentId
) : CombatEvent

data class SkillUsed(
    val actorInstanceId: InstanceId,
    val skillId: ContentId,
    val targetInstanceIds: List<InstanceId> = emptyList()
) : CombatEvent {
    init {
        require(targetInstanceIds.size == targetInstanceIds.toSet().size) {
            "SkillUsed.targetInstanceIds cannot contain duplicates"
        }
    }
}

data class DamageDealt(
    val sourceInstanceId: InstanceId? = null,
    val targetInstanceId: InstanceId,
    val amount: GameNumber,
    val damageKindId: ContentId? = null,
    val critical: Boolean = false
) : CombatEvent

data class HealingApplied(
    val sourceInstanceId: InstanceId? = null,
    val targetInstanceId: InstanceId,
    val amount: GameNumber
) : CombatEvent

data class StatusApplied(
    val statusDefinitionId: ContentId,
    val statusInstanceId: InstanceId,
    val sourceInstanceId: InstanceId? = null,
    val targetInstanceId: InstanceId
) : CombatEvent

data class StatusExpired(
    val statusDefinitionId: ContentId,
    val statusInstanceId: InstanceId,
    val targetInstanceId: InstanceId
) : CombatEvent

data class EnemyKilled(
    val enemyInstanceId: InstanceId,
    val enemyDefinitionId: ContentId,
    val killerInstanceId: InstanceId? = null
) : CombatEvent

data class PlayerDefeated(
    val playerInstanceId: InstanceId
) : CombatEvent

data class CombatEnded(
    val combatSequenceId: Long,
    val reason: CombatEndReason
) : CombatEvent {
    init {
        require(combatSequenceId > 0L) {
            "CombatEnded.combatSequenceId must be positive: $combatSequenceId"
        }
    }
}
