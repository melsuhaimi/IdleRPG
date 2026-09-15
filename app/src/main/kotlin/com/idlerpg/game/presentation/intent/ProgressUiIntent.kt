package com.idlerpg.game.presentation.intent

import com.idlerpg.game.core.id.ContentId
import com.idlerpg.game.domain.command.AllocateRebirthPoints
import com.idlerpg.game.domain.command.ClaimAchievementReward
import com.idlerpg.game.domain.command.ClaimQuestReward
import com.idlerpg.game.domain.command.CommandCorrelationId
import com.idlerpg.game.domain.command.CommitChronicleCollapse
import com.idlerpg.game.domain.command.GameCommand
import com.idlerpg.game.domain.command.PurchaseEchoOffer
import com.idlerpg.game.domain.command.PerformRebirth as PerformRebirthCommand
import com.idlerpg.game.domain.command.PurchaseUpgrade
import com.idlerpg.game.domain.command.RequestChroniclePreview
import com.idlerpg.game.domain.command.ResetRebirthAllocations
import com.idlerpg.game.domain.model.rebirth.RebirthPointPool
import com.idlerpg.game.domain.model.rebirth.RebirthStat

/** FUI-09 player intents for Progress-owned backend commands. */
sealed interface ProgressUiIntent {
    data class ClaimQuest(val questId: ContentId) : ProgressUiIntent
    data class ClaimAchievement(val achievementId: ContentId) : ProgressUiIntent
    data class PurchaseEcho(val offerId: ContentId) : ProgressUiIntent
    data class PurchaseCoreGrowth(val upgradeId: ContentId, val quantity: Long) : ProgressUiIntent {
        init { require(quantity > 0L) }
    }
    data object RequestChronicle : ProgressUiIntent
    data object ConfirmChronicleCollapse : ProgressUiIntent
    data object PerformRebirth : ProgressUiIntent
    data class AllocateRebirth(
        val pool: RebirthPointPool,
        val stat: RebirthStat,
        val amount: Long
    ) : ProgressUiIntent {
        init { require(amount > 0L) }
    }
    data class ResetRebirth(val pool: RebirthPointPool) : ProgressUiIntent
}

/**
 * The only production mapping from Progress gestures to backend commands.
 *
 * [chroniclePreviewToken] is required only for [ProgressUiIntent.ConfirmChronicleCollapse].
 * It must be the opaque token captured from ChroniclePreviewPrepared; presentation never
 * calculates, increments, refreshes, or substitutes that value.
 */
fun ProgressUiIntent.toGameCommand(
    correlationId: CommandCorrelationId,
    chroniclePreviewToken: Long? = null
): GameCommand = when (this) {
    is ProgressUiIntent.ClaimQuest -> ClaimQuestReward(
        questId = questId,
        correlationId = correlationId
    )
    is ProgressUiIntent.ClaimAchievement -> ClaimAchievementReward(
        achievementId = achievementId,
        correlationId = correlationId
    )
    is ProgressUiIntent.PurchaseEcho -> PurchaseEchoOffer(
        offerId = offerId,
        correlationId = correlationId
    )
    is ProgressUiIntent.PurchaseCoreGrowth -> PurchaseUpgrade(
        upgradeId = upgradeId,
        quantity = quantity,
        correlationId = correlationId
    )
    ProgressUiIntent.RequestChronicle -> RequestChroniclePreview(
        correlationId = correlationId
    )
    ProgressUiIntent.PerformRebirth -> PerformRebirthCommand(
        correlationId = correlationId
    )
    is ProgressUiIntent.AllocateRebirth -> AllocateRebirthPoints(
        pool = pool,
        stat = stat,
        amount = amount,
        correlationId = correlationId
    )
    is ProgressUiIntent.ResetRebirth -> ResetRebirthAllocations(
        pool = pool,
        correlationId = correlationId
    )
    ProgressUiIntent.ConfirmChronicleCollapse -> CommitChronicleCollapse(
        expectedPreviewToken = requireNotNull(chroniclePreviewToken) {
            "Chronicle confirmation requires the exact displayed preview token"
        },
        correlationId = correlationId
    )
}
