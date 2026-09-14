package com.idlerpg.game.domain.system.resonance

import com.idlerpg.game.domain.definition.resonance.ConvergenceDefinition
import com.idlerpg.game.domain.event.ConvergenceDiscovered
import com.idlerpg.game.domain.event.GameEvent
import com.idlerpg.game.domain.model.GameState

/** Result of checking persistent first-discovery state for one triggered Convergence. */
data class ConvergenceDiscoveryResult(
    val state: GameState,
    val events: List<GameEvent>,
    val newlyDiscovered: Boolean
)

/** Records Convergence discovery in MetaState exactly once. */
object ConvergenceDiscoverySystem {

    fun recordIfNeeded(
        state: GameState,
        definition: ConvergenceDefinition
    ): ConvergenceDiscoveryResult {
        if (!definition.discoverOnFirstTrigger) {
            return ConvergenceDiscoveryResult(state, emptyList(), false)
        }

        val discoveries = state.meta.discoveries
        if (definition.id in discoveries.discoveredConvergenceIds) {
            return ConvergenceDiscoveryResult(state, emptyList(), false)
        }

        val updatedState = state.copy(
            meta = state.meta.copy(
                discoveries = discoveries.copy(
                    discoveredConvergenceIds =
                        discoveries.discoveredConvergenceIds + definition.id
                )
            )
        )

        return ConvergenceDiscoveryResult(
            state = updatedState,
            events = listOf(ConvergenceDiscovered(definition.id)),
            newlyDiscovered = true
        )
    }
}
