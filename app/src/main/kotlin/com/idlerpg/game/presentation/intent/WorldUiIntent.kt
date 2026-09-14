package com.idlerpg.game.presentation.intent

import com.idlerpg.game.core.id.ContentId
import com.idlerpg.game.domain.command.CommandCorrelationId
import com.idlerpg.game.domain.command.GameCommand
import com.idlerpg.game.domain.command.RetreatEncounter
import com.idlerpg.game.domain.command.SelectRegion
import com.idlerpg.game.domain.command.StartEncounter
import com.idlerpg.game.domain.command.ConfigureWorldAutomation
import com.idlerpg.game.domain.model.world.WorldAutomationMode
import com.idlerpg.game.domain.model.world.PushFailurePolicy

/** FUI-05 World mutations. Navigation to Gear is presentation-only and is not a GameCommand. */
sealed interface WorldUiIntent {
    data class SelectRegionIntent(val regionId: ContentId) : WorldUiIntent
    data class StartEncounterIntent(val encounterId: ContentId) : WorldUiIntent
    data object RetreatEncounterIntent : WorldUiIntent
    data class ConfigureAutomationIntent(
        val mode: WorldAutomationMode,
        val farmEncounterId: ContentId? = null,
        val failurePolicy: PushFailurePolicy = PushFailurePolicy.FARM_HIGHEST_CLEARED
    ) : WorldUiIntent
}

fun WorldUiIntent.toGameCommand(correlationId: CommandCorrelationId): GameCommand =
    when (this) {
        is WorldUiIntent.SelectRegionIntent -> SelectRegion(
            regionId = regionId,
            correlationId = correlationId
        )
        is WorldUiIntent.StartEncounterIntent -> StartEncounter(
            encounterId = encounterId,
            correlationId = correlationId
        )
        WorldUiIntent.RetreatEncounterIntent -> RetreatEncounter(
            correlationId = correlationId
        )
        is WorldUiIntent.ConfigureAutomationIntent -> ConfigureWorldAutomation(
            mode = mode,
            farmEncounterId = farmEncounterId,
            failurePolicy = failurePolicy,
            correlationId = correlationId
        )
    }
