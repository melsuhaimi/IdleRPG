package com.idlerpg.game.domain.system.progression

import com.idlerpg.game.data.content.ContentRegistry
import com.idlerpg.game.domain.definition.progression.FeatureUnlockDefinition
import com.idlerpg.game.domain.definition.progression.FeatureUnlockScope
import com.idlerpg.game.domain.event.FeatureUnlocked
import com.idlerpg.game.domain.event.GameEvent
import com.idlerpg.game.domain.model.GameState

/** Owns deterministic stable-ID feature unlock evaluation across run/meta scopes. */
object FeatureUnlockSystem {

    fun unlockEligible(
        state: GameState,
        contentRegistry: ContentRegistry
    ): ProgressionTransitionResult {
        // Shop capabilities can only be granted by their purchase owner. In particular,
        // a META definition with default level 1 is not a free level-up unlock.
        val purchaseOnly = contentRegistry.allEchoOffers().flatMap { it.effects }
            .filterIsInstance<com.idlerpg.game.domain.definition.chronicle.EchoUnlockEffect.UnlockPersistentFeature>()
            .map { it.featureId }.toSet()
        var transitioned = state
        val events = mutableListOf<GameEvent>()

        contentRegistry.allFeatureUnlocks()
            .sortedWith(
                compareBy<FeatureUnlockDefinition> { it.requiredPlayerLevel }
                    .thenBy { it.id }
            )
            .forEach { definition ->
                if (definition.id in purchaseOnly || isUnlocked(transitioned, definition)) return@forEach
                if (!requirementsMet(transitioned, definition, contentRegistry)) {
                    return@forEach
                }

                transitioned = when (definition.scope) {
                    FeatureUnlockScope.RUN -> transitioned.copy(
                        run = transitioned.run.copy(
                            progression = transitioned.run.progression.copy(
                                featureUnlocks = transitioned.run.progression.featureUnlocks.copy(
                                    unlockedFeatureIds =
                                        transitioned.run.progression.featureUnlocks.unlockedFeatureIds +
                                            definition.id
                                )
                            )
                        )
                    )

                    FeatureUnlockScope.META -> transitioned.copy(
                        meta = transitioned.meta.copy(
                            persistentFeatureUnlocks =
                                transitioned.meta.persistentFeatureUnlocks.copy(
                                    unlockedFeatureIds =
                                        transitioned.meta.persistentFeatureUnlocks.unlockedFeatureIds +
                                            definition.id
                                )
                        )
                    )
                }
                events += FeatureUnlocked(definition.id)
            }

        return ProgressionTransitionResult(transitioned, events)
    }

    fun isUnlocked(
        state: GameState,
        definition: FeatureUnlockDefinition
    ): Boolean = when (definition.scope) {
        FeatureUnlockScope.RUN ->
            definition.id in state.run.progression.featureUnlocks.unlockedFeatureIds

        FeatureUnlockScope.META ->
            definition.id in state.meta.persistentFeatureUnlocks.unlockedFeatureIds
    }

    private fun requirementsMet(
        state: GameState,
        definition: FeatureUnlockDefinition,
        contentRegistry: ContentRegistry
    ): Boolean {
        if (state.run.progression.playerLevel.level < definition.requiredPlayerLevel) {
            return false
        }

        return definition.requiredMasteryLevels
            .entries
            .sortedBy { it.key }
            .all { (affinityId, requiredLevel) ->
                AffinityMasterySystem.levelFor(
                    state = state,
                    affinityId = affinityId,
                    contentRegistry = contentRegistry
                ) >= requiredLevel
            }
    }
}
