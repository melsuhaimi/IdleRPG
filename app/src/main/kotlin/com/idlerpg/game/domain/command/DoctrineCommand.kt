package com.idlerpg.game.domain.command

import com.idlerpg.game.core.id.InstanceId
import com.idlerpg.game.domain.model.doctrine.DoctrineAction
import com.idlerpg.game.domain.model.doctrine.DoctrineCondition
import com.idlerpg.game.domain.model.doctrine.DoctrineRule

/** Doctrine-related immutable player intent. */
sealed interface DoctrineCommand : GameCommand
enum class DoctrinePreset { BALANCED, AGGRESSIVE, SURVIVAL }
data class ApplyDoctrinePreset(val preset: DoctrinePreset, override val correlationId: CommandCorrelationId? = null) : DoctrineCommand

/** Replace the complete ordered active Doctrine configuration atomically. */
data class ReplaceDoctrine(
    val enabled: Boolean,
    val rules: List<DoctrineRule> = emptyList(),
    override val correlationId: CommandCorrelationId? = null
) : DoctrineCommand

/** Add one rule at the end of the current priority order using a controlled new InstanceId. */
data class AddDoctrineRule(
    val condition: DoctrineCondition,
    val action: DoctrineAction,
    val enabled: Boolean = true,
    override val correlationId: CommandCorrelationId? = null
) : DoctrineCommand

/** Replace one existing rule's payload while preserving its stable InstanceId and position. */
data class ReplaceDoctrineRule(
    val ruleId: InstanceId,
    val condition: DoctrineCondition,
    val action: DoctrineAction,
    val enabled: Boolean = true,
    override val correlationId: CommandCorrelationId? = null
) : DoctrineCommand

/** Remove one owned Doctrine rule. */
data class RemoveDoctrineRule(
    val ruleId: InstanceId,
    override val correlationId: CommandCorrelationId? = null
) : DoctrineCommand

/** Move one owned rule to a zero-based priority index. */
data class MoveDoctrineRule(
    val ruleId: InstanceId,
    val newIndex: Int,
    override val correlationId: CommandCorrelationId? = null
) : DoctrineCommand

/** Enable one owned Doctrine rule without changing its position or payload. */
data class EnableDoctrineRule(
    val ruleId: InstanceId,
    override val correlationId: CommandCorrelationId? = null
) : DoctrineCommand

/** Disable one owned Doctrine rule without changing its position or payload. */
data class DisableDoctrineRule(
    val ruleId: InstanceId,
    override val correlationId: CommandCorrelationId? = null
) : DoctrineCommand
