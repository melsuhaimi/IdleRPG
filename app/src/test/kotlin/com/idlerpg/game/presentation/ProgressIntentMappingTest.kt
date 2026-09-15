package com.idlerpg.game.presentation

import com.idlerpg.game.core.id.ContentId
import com.idlerpg.game.domain.command.AllocateRebirthPoints
import com.idlerpg.game.domain.command.ClaimAchievementReward
import com.idlerpg.game.domain.command.ClaimQuestReward
import com.idlerpg.game.domain.command.CommandCorrelationId
import com.idlerpg.game.domain.command.CommitChronicleCollapse
import com.idlerpg.game.domain.command.PurchaseEchoOffer
import com.idlerpg.game.domain.command.PerformRebirth
import com.idlerpg.game.domain.command.PurchaseUpgrade
import com.idlerpg.game.domain.command.RequestChroniclePreview
import com.idlerpg.game.domain.command.ResetRebirthAllocations
import com.idlerpg.game.domain.model.rebirth.RebirthPointPool
import com.idlerpg.game.domain.model.rebirth.RebirthStat
import com.idlerpg.game.presentation.intent.ProgressUiIntent
import com.idlerpg.game.presentation.intent.toGameCommand

object ProgressIntentMappingTest {
    @JvmStatic
    fun main(args: Array<String>) {
        val correlationId = CommandCorrelationId(909L)
        val questId = ContentId("quest.training.first_hunt")
        val achievementId = ContentId("achievement.convergence.first_forged_flame")
        val echoOfferId = ContentId("echo_unlock.adaptation_forecast")
        val previewToken = 9_090_909L

        check(
            ProgressUiIntent.ClaimQuest(questId).toGameCommand(correlationId) ==
                ClaimQuestReward(questId, correlationId)
        )
        check(
            ProgressUiIntent.ClaimAchievement(achievementId).toGameCommand(correlationId) ==
                ClaimAchievementReward(achievementId, correlationId)
        )
        check(
            ProgressUiIntent.PurchaseEcho(echoOfferId).toGameCommand(correlationId) ==
                PurchaseEchoOffer(echoOfferId, correlationId)
        )
        check(
            ProgressUiIntent.PurchaseCoreGrowth(ContentId("upgrade.precision"), 25L)
                .toGameCommand(correlationId) ==
                PurchaseUpgrade(ContentId("upgrade.precision"), 25L, correlationId)
        )
        check(runCatching {
            ProgressUiIntent.PurchaseCoreGrowth(ContentId("upgrade.precision"), 0L)
        }.isFailure)
        check(
            ProgressUiIntent.RequestChronicle.toGameCommand(correlationId) ==
                RequestChroniclePreview(correlationId)
        )
        check(
            ProgressUiIntent.PerformRebirth.toGameCommand(correlationId) ==
                PerformRebirth(correlationId)
        )
        check(
            ProgressUiIntent.AllocateRebirth(
                pool = RebirthPointPool.NORMAL,
                stat = RebirthStat.CRITICAL_CHANCE,
                amount = 3L
            ).toGameCommand(correlationId) ==
                AllocateRebirthPoints(
                    pool = RebirthPointPool.NORMAL,
                    stat = RebirthStat.CRITICAL_CHANCE,
                    amount = 3L,
                    correlationId = correlationId
                )
        )
        check(
            ProgressUiIntent.ResetRebirth(RebirthPointPool.LEGACY)
                .toGameCommand(correlationId) ==
                ResetRebirthAllocations(RebirthPointPool.LEGACY, correlationId)
        )
        check(
            ProgressUiIntent.ConfirmChronicleCollapse.toGameCommand(
                correlationId = correlationId,
                chroniclePreviewToken = previewToken
            ) == CommitChronicleCollapse(previewToken, correlationId)
        )

        println("FUI09_PROGRESS_INTENT_MAPPING_PASS")
    }
}
