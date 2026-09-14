package com.idlerpg.game.domain.event

import com.idlerpg.game.core.id.ContentId
import com.idlerpg.game.core.id.InstanceId
import com.idlerpg.game.core.number.GameNumber

/** Completed Resonance/Convergence facts. */
sealed interface ResonanceEvent : GameEvent

data class ResonanceGenerated(
    val affinityId: ContentId,
    val amount: GameNumber,
    val sourceDefinitionId: ContentId,
    val sourceInstanceId: InstanceId? = null
) : ResonanceEvent

data class ResonanceConsumed(
    val affinityId: ContentId,
    val amount: GameNumber,
    val convergenceId: ContentId? = null
) : ResonanceEvent

data class ConvergenceTriggered(
    val convergenceId: ContentId,
    val sourceActionId: ContentId? = null
) : ResonanceEvent

data class ConvergenceDiscovered(
    val convergenceId: ContentId
) : ResonanceEvent
