package com.idlerpg.game.domain.system.chronicle

import com.idlerpg.game.domain.definition.chronicle.EchoUnlockDefinition
import com.idlerpg.game.domain.definition.chronicle.EchoUnlockEffect
import com.idlerpg.game.domain.event.DiscoveryUnlocked
import com.idlerpg.game.domain.event.FeatureUnlocked
import com.idlerpg.game.domain.event.GameEvent
import com.idlerpg.game.domain.model.MetaState

/** Atomic persistent knowledge/capability updates for Chronicle-level discovery. */
object DiscoverySystem {

    data class Result(
        val meta: MetaState,
        val events: List<GameEvent>
    )

    fun areEffectsFullyApplied(
        meta: MetaState,
        effects: List<EchoUnlockEffect>
    ): Boolean = effects.all { effect ->
        when (effect) {
            is EchoUnlockEffect.RevealHiddenContent ->
                effect.contentId in meta.discoveries.unlockedHiddenContentIds

            is EchoUnlockEffect.UnlockPersistentFeature ->
                effect.featureId in meta.persistentFeatureUnlocks.unlockedFeatureIds
        }
    }

    fun applyEffects(
        meta: MetaState,
        effects: List<EchoUnlockEffect>
    ): Result {
        var current = meta
        val events = mutableListOf<GameEvent>()

        effects.forEach { effect ->
            when (effect) {
                is EchoUnlockEffect.RevealHiddenContent -> {
                    if (effect.contentId !in current.discoveries.unlockedHiddenContentIds) {
                        current = current.copy(
                            discoveries = current.discoveries.copy(
                                unlockedHiddenContentIds =
                                    current.discoveries.unlockedHiddenContentIds + effect.contentId
                            )
                        )
                        events += DiscoveryUnlocked(effect.contentId)
                    }
                }

                is EchoUnlockEffect.UnlockPersistentFeature -> {
                    if (effect.featureId !in current.persistentFeatureUnlocks.unlockedFeatureIds) {
                        current = current.copy(
                            persistentFeatureUnlocks = current.persistentFeatureUnlocks.copy(
                                unlockedFeatureIds =
                                    current.persistentFeatureUnlocks.unlockedFeatureIds + effect.featureId
                            )
                        )
                        events += FeatureUnlocked(effect.featureId)
                    }
                }
            }
        }

        return Result(
            meta = current,
            events = events
        )
    }

    /** Legacy Foundation-17 helper retained for migration/compatibility code. */
    fun isFullyApplied(
        meta: MetaState,
        definition: EchoUnlockDefinition
    ): Boolean = areEffectsFullyApplied(meta, definition.effects)

    /** Legacy Foundation-17 helper retained for migration/compatibility code. */
    fun applyEchoUnlock(
        meta: MetaState,
        definition: EchoUnlockDefinition
    ): Result = applyEffects(meta, definition.effects)
}
