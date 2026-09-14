package com.idlerpg.game.presentation.intent

import com.idlerpg.game.core.id.ContentId
import com.idlerpg.game.domain.command.ClearQueuedSkillCast
import com.idlerpg.game.domain.command.CommandCorrelationId
import com.idlerpg.game.domain.command.GameCommand
import com.idlerpg.game.domain.command.PurchaseUpgrade
import com.idlerpg.game.domain.command.QueueSkillCast
import com.idlerpg.game.domain.command.RetreatEncounter
import com.idlerpg.game.domain.command.StartEncounter
import com.idlerpg.game.domain.command.DeployStartingEncounter

/**
 * FUI-03 Battle-only presentation intents.
 *
 * Every gameplay-changing intent maps to exactly one backend [GameCommand]. This layer
 * never mutates combat, currencies, cooldowns, Resonance, or encounter state directly.
 */
sealed interface BattleUiIntent {
    data class QueueSkill(
        val skillId: ContentId
    ) : BattleUiIntent

    object ClearQueuedSkill : BattleUiIntent

    data class PurchaseUpgradeLevel(
        val upgradeId: ContentId
    ) : BattleUiIntent

    object Retreat : BattleUiIntent

    object DeployStartingEncounter : BattleUiIntent

    data class RetryEncounter(
        val encounterId: ContentId
    ) : BattleUiIntent
}

fun BattleUiIntent.toGameCommand(
    correlationId: CommandCorrelationId
): GameCommand = when (this) {
    is BattleUiIntent.QueueSkill -> QueueSkillCast(
        skillId = skillId,
        correlationId = correlationId
    )
    BattleUiIntent.ClearQueuedSkill -> ClearQueuedSkillCast(
        correlationId = correlationId
    )
    is BattleUiIntent.PurchaseUpgradeLevel -> PurchaseUpgrade(
        upgradeId = upgradeId,
        quantity = 1L,
        correlationId = correlationId
    )
    BattleUiIntent.Retreat -> RetreatEncounter(
        correlationId = correlationId
    )
    BattleUiIntent.DeployStartingEncounter -> DeployStartingEncounter(
        correlationId = correlationId
    )
    is BattleUiIntent.RetryEncounter -> StartEncounter(
        encounterId = encounterId,
        correlationId = correlationId
    )
}
