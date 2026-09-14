package com.idlerpg.game.domain.system.resonance

import com.idlerpg.game.core.id.ContentId
import com.idlerpg.game.core.id.InstanceId
import com.idlerpg.game.core.number.GameNumber
import com.idlerpg.game.domain.definition.Affinity
import com.idlerpg.game.domain.definition.resonance.ResonanceEmissionDefinition
import com.idlerpg.game.domain.event.GameEvent
import com.idlerpg.game.domain.event.ResonanceGenerated
import com.idlerpg.game.domain.model.GameState
import com.idlerpg.game.domain.model.resonance.ConvergenceState
import com.idlerpg.game.domain.model.resonance.ResonancePulse

/** Result of emitting an action's declared Resonance pulses. */
data class ResonanceEmissionResult(
    val state: GameState,
    val events: List<GameEvent>,
    val pulses: List<ResonancePulse>
)

/** Owns run-local Resonance charge and rolling sequence mutation. */
object ResonanceSystem {

    fun beginEncounter(state: GameState): GameState =
        state.copy(
            run = state.run.copy(
                resonance = state.run.resonance.copy(
                    convergence = state.run.resonance.convergence.copy(
                        encounterTriggerCountById = emptyMap()
                    )
                )
            )
        )

    fun charge(
        state: GameState,
        affinity: Affinity
    ): GameNumber =
        state.run.resonance.chargeByAffinityId[affinity.id] ?: GameNumber.ZERO

    fun emitAll(
        state: GameState,
        emissions: List<ResonanceEmissionDefinition>,
        sourceDefinitionId: ContentId,
        sourceInstanceId: InstanceId?,
        maximumChargePerAffinity: GameNumber,
        sequenceMaximumSize: Int
    ): ResonanceEmissionResult {
        require(maximumChargePerAffinity > GameNumber.ZERO) {
            "maximumChargePerAffinity must be > 0"
        }
        require(sequenceMaximumSize > 0) {
            "sequenceMaximumSize must be > 0"
        }

        var currentState = state
        val events = mutableListOf<GameEvent>()
        val pulses = mutableListOf<ResonancePulse>()

        for (emission in emissions) {
            val resonance = currentState.run.resonance
            val currentCharge =
                resonance.chargeByAffinityId[emission.affinity.id] ?: GameNumber.ZERO
            val requested = currentCharge + emission.amount
            val bounded =
                if (requested > maximumChargePerAffinity) {
                    maximumChargePerAffinity
                } else {
                    requested
                }

            val sequence = ResonanceSequenceSystem.append(
                state = resonance.sequence,
                affinity = emission.affinity,
                maximumSize = sequenceMaximumSize
            )

            currentState = currentState.copy(
                run = currentState.run.copy(
                    resonance = resonance.copy(
                        chargeByAffinityId = resonance.chargeByAffinityId +
                            (emission.affinity.id to bounded),
                        sequence = sequence
                    )
                )
            )

            val pulse = ResonancePulse(
                affinity = emission.affinity,
                amount = emission.amount,
                sourceDefinitionId = sourceDefinitionId,
                sourceInstanceId = sourceInstanceId,
                emittedAt = currentState.engine.simulationTime
            )
            pulses += pulse
            events += ResonanceGenerated(
                affinityId = emission.affinity.id,
                amount = emission.amount,
                sourceDefinitionId = sourceDefinitionId,
                sourceInstanceId = sourceInstanceId
            )
        }

        return ResonanceEmissionResult(
            state = currentState,
            events = events,
            pulses = pulses
        )
    }
}
