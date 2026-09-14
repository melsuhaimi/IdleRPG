package com.idlerpg.game.domain.command

import com.idlerpg.game.core.id.ContentId
import com.idlerpg.game.domain.model.world.PushFailurePolicy
import com.idlerpg.game.domain.model.world.WorldAutomationMode

/** World/encounter player intent. No world behavior is implemented in Foundation 3. */
sealed interface WorldCommand : GameCommand

/** Attempt to make a region the active region. */
data class SelectRegion(
    val regionId: ContentId,
    override val correlationId: CommandCorrelationId? = null
) : WorldCommand

/** Attempt to start one authored encounter definition. */
data class StartEncounter(
    val encounterId: ContentId,
    override val correlationId: CommandCorrelationId? = null
) : WorldCommand

/** Selects the first authored starting region and deploys its next progression encounter. */
data class DeployStartingEncounter(
    override val correlationId: CommandCorrelationId? = null
) : WorldCommand

/** Attempt to retreat from the current encounter. */
data class RetreatEncounter(
    override val correlationId: CommandCorrelationId? = null
) : WorldCommand

data class ConfigureWorldAutomation(
    val mode: WorldAutomationMode,
    val farmEncounterId: ContentId? = null,
    val failurePolicy: PushFailurePolicy = PushFailurePolicy.FARM_HIGHEST_CLEARED,
    override val correlationId: CommandCorrelationId? = null
) : WorldCommand
