package com.idlerpg.game.domain.model.adaptation

import com.idlerpg.game.core.number.GameNumber

/**
 * Canonical ecological pressure for one affinity inside one region.
 *
 * [currentEncounterContribution] is resumable transient encounter evidence. It is
 * accumulated from effective affinity contribution during the active encounter and is
 * committed to [pressure] only when that encounter completes successfully.
 *
 * [recentEncounterContribution] is a bounded one-encounter summary used for diagnostics
 * and later presentation/balance analysis. It does not grow without bound.
 */
data class AffinityExposureState(
    val pressure: GameNumber = GameNumber.ZERO,
    val currentEncounterContribution: GameNumber = GameNumber.ZERO,
    val recentEncounterContribution: GameNumber = GameNumber.ZERO
)
