package com.idlerpg.game.domain.system.adaptation

import com.idlerpg.game.core.config.BalanceConfig
import com.idlerpg.game.core.id.ContentId
import com.idlerpg.game.core.number.GameMath
import com.idlerpg.game.core.number.GameNumber
import com.idlerpg.game.data.content.ContentRegistry
import com.idlerpg.game.domain.definition.Affinity
import com.idlerpg.game.domain.event.AdaptationExposureRecorded
import com.idlerpg.game.domain.event.AdaptationTierChanged
import com.idlerpg.game.domain.event.GameEvent
import com.idlerpg.game.domain.model.GameState
import com.idlerpg.game.domain.model.adaptation.AffinityExposureState
import com.idlerpg.game.domain.model.adaptation.RegionAdaptationState

/** Result of aggregating one successfully completed encounter into regional ecology. */
data class AdaptationEncounterResult(
    val state: GameState,
    val events: List<GameEvent>
)

/**
 * Owns encounter-boundary Adaptation pressure, decay, and tier transitions.
 *
 * Active enemies are never mutated here. The resulting regional snapshot is consumed only
 * by a later EnemyFactory spawn, preserving the "future enemies adapt" architecture law.
 */
object AdaptationSystem {

    fun completeEncounter(
        state: GameState,
        regionId: ContentId,
        contentRegistry: ContentRegistry,
        balanceConfig: BalanceConfig
    ): AdaptationEncounterResult {
        val regionDefinition = contentRegistry.region(regionId)
        val thresholdId = regionDefinition.adaptationThresholdDefinitionId
            ?: return AdaptationEncounterResult(
                state = AdaptationExposureSystem.discardCurrentEncounter(state, regionId),
                events = emptyList()
            )
        val thresholds = contentRegistry.adaptationThreshold(thresholdId)
        val pressureCap = thresholds.minimumPressureByTier.values.maxOrNull()!!

        val adaptation = state.run.adaptation
        val previousRegion =
            adaptation.regionStateById[regionId] ?: RegionAdaptationState()
        var exposures = previousRegion.exposureByAffinityId
        var tiers = previousRegion.tierByAffinityId
        val events = mutableListOf<GameEvent>()

        val affinities = Affinity.values().sortedBy { it.id }
        for (affinity in affinities) {
            val previousExposure =
                exposures[affinity.id] ?: AffinityExposureState()
            val contribution = previousExposure.currentEncounterContribution
            // Normalize legacy backlogs before decay so changing builds has an immediate effect.
            val boundedPressure = minOf(previousExposure.pressure, pressureCap)

            val nextPressure =
                if (contribution > GameNumber.ZERO) {
                    val gain = GameMath.applyRatio(
                        value = contribution,
                        ratio = balanceConfig.adaptationExposureGainRatio
                    )
                    if (gain > GameNumber.ZERO) {
                        events += AdaptationExposureRecorded(
                            regionId = regionId,
                            affinityId = affinity.id,
                            amount = gain
                        )
                    }
                    minOf(boundedPressure + gain, pressureCap)
                } else {
                    subtractFloorZero(
                        value = boundedPressure,
                        decrement = maxOf(balanceConfig.adaptationUnusedAffinityDecayPerEncounter,
                            boundedPressure.divide(50L))
                    )
                }

            val previousTier = tiers[affinity.id] ?: 0
            val nextTier = thresholds.tierForPressure(nextPressure)
            if (nextTier != previousTier) {
                events += AdaptationTierChanged(
                    regionId = regionId,
                    affinityId = affinity.id,
                    previousTier = previousTier,
                    newTier = nextTier
                )
            }

            val nextExposure = previousExposure.copy(
                pressure = nextPressure,
                currentEncounterContribution = GameNumber.ZERO,
                recentEncounterContribution = contribution
            )

            val shouldKeepExposure =
                nextExposure.pressure > GameNumber.ZERO ||
                    nextExposure.recentEncounterContribution > GameNumber.ZERO

            exposures =
                if (shouldKeepExposure) {
                    exposures + (affinity.id to nextExposure)
                } else {
                    exposures - affinity.id
                }

            tiers =
                if (nextTier > 0) {
                    tiers + (affinity.id to nextTier)
                } else {
                    tiers - affinity.id
                }
        }

        val nextRegion = previousRegion.copy(
            exposureByAffinityId = exposures,
            tierByAffinityId = tiers
        )
        val updatedRegion = nextRegion.copy(
            unlockedMutationIds = MutationSystem.eligibleMutationIds(
                regionDefinition = regionDefinition,
                regionState = nextRegion,
                contentRegistry = contentRegistry
            )
        )

        val updatedState = state.copy(
            run = state.run.copy(
                adaptation = adaptation.copy(
                    regionStateById = adaptation.regionStateById +
                        (regionId to updatedRegion)
                )
            )
        )

        return AdaptationEncounterResult(
            state = updatedState,
            events = events
        )
    }

    private fun subtractFloorZero(
        value: GameNumber,
        decrement: GameNumber
    ): GameNumber =
        if (value > decrement) {
            value - decrement
        } else {
            GameNumber.ZERO
        }
}
