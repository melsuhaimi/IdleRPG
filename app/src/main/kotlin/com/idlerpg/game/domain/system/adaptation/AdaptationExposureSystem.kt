package com.idlerpg.game.domain.system.adaptation

import com.idlerpg.game.core.id.ContentId
import com.idlerpg.game.core.number.GameNumber
import com.idlerpg.game.domain.definition.Affinity
import com.idlerpg.game.domain.model.GameState
import com.idlerpg.game.domain.model.adaptation.AffinityExposureState
import com.idlerpg.game.domain.model.adaptation.RegionAdaptationState
import java.math.BigInteger

/**
 * Observes effective affinity contribution without changing long-term pressure mid-fight.
 *
 * Records effective damage and player healing. Raw skill presses and overhealing are
 * ignored so actions without an effect cannot inflate Adaptation pressure.
 */
object AdaptationExposureSystem {

    fun beginEncounter(
        state: GameState,
        regionId: ContentId
    ): GameState =
        updateRegion(state, regionId) { region ->
            region.copy(
                exposureByAffinityId = region.exposureByAffinityId.mapValues { (_, exposure) ->
                    exposure.copy(currentEncounterContribution = GameNumber.ZERO)
                }
            )
        }

    fun discardCurrentEncounter(
        state: GameState,
        regionId: ContentId
    ): GameState =
        beginEncounter(state, regionId)

    /**
     * Adds [effectiveAmount] across the supplied affinity sequence without double counting.
     *
     * If a contribution has multiple affinity symbols, the integer amount is split evenly.
     * Any remainder is assigned in stable ContentId order. Duplicate symbols intentionally
     * receive proportional shares before being folded back into one affinity total.
     */
    fun observeEffectiveContribution(
        state: GameState,
        affinitySequence: List<Affinity>,
        effectiveAmount: GameNumber
    ): GameState {
        if (affinitySequence.isEmpty() || effectiveAmount == GameNumber.ZERO) {
            return state
        }

        val regionId = state.run.world.activeRegionId
            ?: return state

        val stableSymbols = affinitySequence.sortedBy { it.id }
        val symbolCount = stableSymbols.size
        val total = effectiveAmount.toBigInteger()
        val divisor = BigInteger.valueOf(symbolCount.toLong())
        val quotient = total.divide(divisor)
        val remainder = total.remainder(divisor).toInt()

        val contributionByAffinity = linkedMapOf<ContentId, GameNumber>()
        stableSymbols.forEachIndexed { index, affinity ->
            val symbolAmount =
                quotient + if (index < remainder) BigInteger.ONE else BigInteger.ZERO
            if (symbolAmount.signum() > 0) {
                val id = affinity.id
                val current = contributionByAffinity[id] ?: GameNumber.ZERO
                contributionByAffinity[id] =
                    current + GameNumber.fromBigInteger(symbolAmount)
            }
        }

        if (contributionByAffinity.isEmpty()) {
            return state
        }

        return updateRegion(state, regionId) { region ->
            var exposures = region.exposureByAffinityId

            for ((affinityId, amount) in contributionByAffinity.entries.sortedBy { it.key }) {
                val current = exposures[affinityId] ?: AffinityExposureState()
                exposures = exposures + (
                    affinityId to current.copy(
                        currentEncounterContribution =
                            current.currentEncounterContribution + amount
                    )
                )
            }

            region.copy(exposureByAffinityId = exposures)
        }
    }

    private fun updateRegion(
        state: GameState,
        regionId: ContentId,
        transform: (RegionAdaptationState) -> RegionAdaptationState
    ): GameState {
        val adaptation = state.run.adaptation
        val existing =
            adaptation.regionStateById[regionId] ?: RegionAdaptationState()
        val updated = transform(existing)

        return state.copy(
            run = state.run.copy(
                adaptation = adaptation.copy(
                    regionStateById = adaptation.regionStateById +
                        (regionId to updated)
                )
            )
        )
    }
}
