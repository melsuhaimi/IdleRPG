package com.idlerpg.game.presentation.intent

import com.idlerpg.game.core.id.InstanceId
import com.idlerpg.game.domain.command.AddDoctrineRule
import com.idlerpg.game.domain.command.CommandCorrelationId
import com.idlerpg.game.domain.command.DisableDoctrineRule
import com.idlerpg.game.domain.command.EnableDoctrineRule
import com.idlerpg.game.domain.command.GameCommand
import com.idlerpg.game.domain.command.MoveDoctrineRule
import com.idlerpg.game.domain.command.RemoveDoctrineRule
import com.idlerpg.game.domain.command.ReplaceDoctrine
import com.idlerpg.game.domain.command.ReplaceDoctrineRule
import com.idlerpg.game.domain.command.ApplyDoctrinePreset
import com.idlerpg.game.domain.command.DoctrinePreset
import com.idlerpg.game.domain.model.GameState
import com.idlerpg.game.domain.model.doctrine.DoctrineAction
import com.idlerpg.game.domain.model.doctrine.DoctrineCondition

/** FUI-06 gameplay-changing Doctrine intents. Draft editing itself remains presentation-only. */
sealed interface DoctrineUiIntent {
    data class ApplyPreset(val preset: DoctrinePreset) : DoctrineUiIntent
    data class SetDoctrineEnabled(val enabled: Boolean) : DoctrineUiIntent

    data class AddRule(
        val condition: DoctrineCondition,
        val action: DoctrineAction,
        val enabled: Boolean
    ) : DoctrineUiIntent

    data class ReplaceRule(
        val ruleId: InstanceId,
        val condition: DoctrineCondition,
        val action: DoctrineAction,
        val enabled: Boolean
    ) : DoctrineUiIntent

    data class RemoveRule(val ruleId: InstanceId) : DoctrineUiIntent
    data class MoveRule(val ruleId: InstanceId, val newIndex: Int) : DoctrineUiIntent
    data class SetRuleEnabled(val ruleId: InstanceId, val enabled: Boolean) : DoctrineUiIntent
}

/**
 * Maps exactly one player mutation request to exactly one canonical Doctrine command.
 *
 * The root enabled toggle uses ReplaceDoctrine only with the canonical rule list from the
 * current snapshot, so presentation never fabricates rule InstanceIds.
 */
fun DoctrineUiIntent.toGameCommand(
    correlationId: CommandCorrelationId,
    currentState: GameState
): GameCommand = when (this) {
    is DoctrineUiIntent.ApplyPreset -> ApplyDoctrinePreset(preset, correlationId)
    is DoctrineUiIntent.SetDoctrineEnabled -> ReplaceDoctrine(
        enabled = enabled,
        rules = currentState.run.doctrine.rules,
        correlationId = correlationId
    )
    is DoctrineUiIntent.AddRule -> AddDoctrineRule(
        condition = condition,
        action = action,
        enabled = enabled,
        correlationId = correlationId
    )
    is DoctrineUiIntent.ReplaceRule -> ReplaceDoctrineRule(
        ruleId = ruleId,
        condition = condition,
        action = action,
        enabled = enabled,
        correlationId = correlationId
    )
    is DoctrineUiIntent.RemoveRule -> RemoveDoctrineRule(
        ruleId = ruleId,
        correlationId = correlationId
    )
    is DoctrineUiIntent.MoveRule -> MoveDoctrineRule(
        ruleId = ruleId,
        newIndex = newIndex,
        correlationId = correlationId
    )
    is DoctrineUiIntent.SetRuleEnabled -> if (enabled) {
        EnableDoctrineRule(ruleId = ruleId, correlationId = correlationId)
    } else {
        DisableDoctrineRule(ruleId = ruleId, correlationId = correlationId)
    }
}
