package com.idlerpg.game.domain.system.resonance

import com.idlerpg.game.core.id.ContentId
import com.idlerpg.game.core.id.InstanceId
import com.idlerpg.game.core.number.GameNumber
import com.idlerpg.game.domain.definition.Affinity
import com.idlerpg.game.domain.definition.resonance.ConvergenceDefinition
import com.idlerpg.game.domain.definition.resonance.ResonanceConsumePolicy
import com.idlerpg.game.domain.engine.EngineContext
import com.idlerpg.game.domain.event.ConvergenceTriggered
import com.idlerpg.game.domain.event.GameEvent
import com.idlerpg.game.domain.event.ResonanceConsumed
import com.idlerpg.game.domain.model.GameState
import com.idlerpg.game.domain.system.combat.ActionResolutionSystem

/** Result of deterministic post-emission Convergence evaluation. */
data class ConvergenceResolutionResult(
    val state: GameState,
    val events: List<GameEvent>,
    val triggeredConvergenceId: ContentId? = null
)

/** Owns deterministic Convergence eligibility, selection, charge consumption, and effects. */
object ConvergenceSystem {

    fun resolveFirstEligible(
        state: GameState,
        sourceActionId: ContentId,
        sourceInstanceId: InstanceId,
        targetInstanceId: InstanceId,
        context: EngineContext
    ): ConvergenceResolutionResult {
        val eligible = context.contentRegistry
            .allConvergences()
            .filter { definition -> isEligible(state, definition) }
            .sortedWith(
                compareByDescending<ConvergenceDefinition> { it.pattern.length }
                    .thenByDescending { it.priority }
                    .thenBy { it.id }
            )

        val selected = eligible.firstOrNull()
            ?: return ConvergenceResolutionResult(
                state = state,
                events = emptyList(),
                triggeredConvergenceId = null
            )

        var currentState = state
        val events = mutableListOf<GameEvent>()

        val consumed = consumeCharge(
            state = currentState,
            definition = selected
        )
        currentState = consumed.state
        events += consumed.events

        currentState = recordTrigger(
            state = currentState,
            definition = selected
        )
        events += ConvergenceTriggered(
            convergenceId = selected.id,
            sourceActionId = sourceActionId
        )

        val discovery = ConvergenceDiscoverySystem.recordIfNeeded(
            state = currentState,
            definition = selected
        )
        currentState = discovery.state
        events += discovery.events

        // Convergence effects resolve through the primitive effect pipeline only.
        // They do not emit secondary Resonance pulses, preventing recursive chains.
        val effects = ActionResolutionSystem.resolveFollowUpEffects(
            state = currentState,
            actorInstanceId = sourceInstanceId,
            targetInstanceId = targetInstanceId,
            effects = selected.effects,
            context = context,
            adaptationAffinitySequence = selected.pattern.affinities
        )
        currentState = effects.state
        events += effects.events

        return ConvergenceResolutionResult(
            state = currentState,
            events = events,
            triggeredConvergenceId = selected.id
        )
    }

    fun isEligible(
        state: GameState,
        definition: ConvergenceDefinition
    ): Boolean {
        if (!ResonanceSequenceSystem.matchesSuffix(
                state = state.run.resonance.sequence,
                pattern = definition.pattern
            )
        ) {
            return false
        }

        val now = state.engine.simulationTime
        val readyAt = state.run.resonance.convergence.readyAtById[definition.id]
        if (readyAt != null && readyAt > now) {
            return false
        }

        val encounterCount =
            state.run.resonance.convergence.encounterTriggerCountById[definition.id] ?: 0L
        val limit = definition.maxTriggersPerEncounter
        if (limit != null && encounterCount >= limit) {
            return false
        }

        for ((affinity, required) in definition.minimumChargeByAffinity) {
            if (ResonanceSystem.charge(state, affinity) < required) {
                return false
            }
        }

        return true
    }

    private data class ChargeConsumptionResult(
        val state: GameState,
        val events: List<GameEvent>
    )

    private fun consumeCharge(
        state: GameState,
        definition: ConvergenceDefinition
    ): ChargeConsumptionResult =
        when (definition.consumePolicy) {
            ResonanceConsumePolicy.NONE ->
                ChargeConsumptionResult(state, emptyList())

            ResonanceConsumePolicy.REQUIRED_CHARGE -> {
                var charges = state.run.resonance.chargeByAffinityId
                val events = mutableListOf<GameEvent>()

                definition.minimumChargeByAffinity
                    .entries
                    .sortedBy { it.key.id }
                    .forEach { (affinity, amount) ->
                        val current = charges[affinity.id] ?: GameNumber.ZERO
                        require(current >= amount) {
                            "Eligible Convergence ${definition.id} lacks required ${affinity.id} charge"
                        }
                        charges = charges + (affinity.id to (current - amount))
                        events += ResonanceConsumed(
                            affinityId = affinity.id,
                            amount = amount,
                            convergenceId = definition.id
                        )
                    }

                ChargeConsumptionResult(
                    state = state.copy(
                        run = state.run.copy(
                            resonance = state.run.resonance.copy(
                                chargeByAffinityId = charges
                            )
                        )
                    ),
                    events = events
                )
            }
        }

    private fun recordTrigger(
        state: GameState,
        definition: ConvergenceDefinition
    ): GameState {
        val resonance = state.run.resonance
        val convergence = resonance.convergence
        val total = convergence.triggerCountById[definition.id] ?: GameNumber.ZERO
        val encounter = convergence.encounterTriggerCountById[definition.id] ?: 0L
        val readyAt = state.engine.simulationTime + definition.cooldown

        return state.copy(
            run = state.run.copy(
                resonance = resonance.copy(
                    convergence = convergence.copy(
                        readyAtById = convergence.readyAtById +
                            (definition.id to readyAt),
                        triggerCountById = convergence.triggerCountById +
                            (definition.id to (total + GameNumber.ONE)),
                        encounterTriggerCountById = convergence.encounterTriggerCountById +
                            (definition.id to Math.addExact(encounter, 1L))
                    )
                )
            )
        )
    }
}
