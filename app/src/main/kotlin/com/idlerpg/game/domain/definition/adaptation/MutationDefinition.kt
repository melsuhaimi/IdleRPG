package com.idlerpg.game.domain.definition.adaptation

import com.idlerpg.game.core.id.ContentId
import com.idlerpg.game.core.number.Ratio
import com.idlerpg.game.domain.definition.Affinity

/**
 * Reusable authored Adaptation mutation.
 *
 * [requiredEnemyTags] is a compatibility filter. An empty set means the mutation may be
 * applied to any enemy. Selection uses [selectionWeight] through the controlled GameRandom.
 */
data class MutationDefinition(
    val id: ContentId,
    val displayName: String,
    val triggerAffinity: Affinity,
    val minimumAdaptationTier: Int,
    val selectionWeight: Long,
    val requiredEnemyTags: Set<ContentId> = emptySet(),
    val effect: MutationEffectDefinition,
    val additionalEffects: List<MutationEffectDefinition> = emptyList()
) {
    init {
        require(displayName.isNotBlank()) {
            "MutationDefinition.displayName cannot be blank for $id"
        }
        require(minimumAdaptationTier > 0) {
            "MutationDefinition.minimumAdaptationTier must be positive for $id"
        }
        require(selectionWeight > 0L) {
            "MutationDefinition.selectionWeight must be positive for $id"
        }
        require(additionalEffects.none { it == effect }) {
            "MutationDefinition.additionalEffects cannot duplicate the primary effect for $id"
        }
    }

    val effects: List<MutationEffectDefinition>
        get() = listOf(effect) + additionalEffects
}

/**
 * Bounded Foundation 10 mutation-effect vocabulary.
 *
 * More mutation primitives may be added by later foundations only when a reusable mechanic
 * cannot be represented by the existing vocabulary.
 */
sealed interface MutationEffectDefinition {

    /**
     * Multiplies damage received from an action carrying [affinity].
     *
     * Example: 50% means the mutated enemy receives half damage from Ember-tagged actions.
     */
    data class DamageTakenMultiplierForAffinity(
        val affinity: Affinity,
        val multiplier: Ratio
    ) : MutationEffectDefinition {
        init {
            require(multiplier > Ratio.ZERO) {
                "DamageTakenMultiplierForAffinity.multiplier must be > 0"
            }
        }
    }

    /** Multiplies this carrier's authored enemy-action interval. Values below 100% act faster. */
    data class EnemyActionIntervalMultiplier(
        val multiplier: Ratio
    ) : MutationEffectDefinition {
        init {
            require(multiplier > Ratio.ZERO && multiplier <= Ratio.ofUnits(20_000L)) {
                "EnemyActionIntervalMultiplier.multiplier must be within (0%, 200%]"
            }
        }
    }

    /** Adds a fixed charge drain to this carrier's completed attack. */
    data class AdditionalResonanceDrain(
        val amount: com.idlerpg.game.core.number.GameNumber
    ) : MutationEffectDefinition {
        init {
            require(amount > com.idlerpg.game.core.number.GameNumber.ZERO) {
                "AdditionalResonanceDrain.amount must be positive"
            }
        }
    }

    /** Strongest living carrier suppresses player healing once; multiple carriers do not stack. */
    data class PlayerHealingMultiplier(
        val multiplier: Ratio
    ) : MutationEffectDefinition {
        init {
            require(multiplier > Ratio.ZERO && multiplier <= Ratio.ONE) {
                "PlayerHealingMultiplier.multiplier must be within (0%, 100%]"
            }
        }
    }

    /** Multiplies this carrier's attack while the player has a status tagged with [affinity]. */
    data class EnemyDamageMultiplierWhilePlayerHasAffinityStatus(
        val affinity: Affinity,
        val multiplier: Ratio
    ) : MutationEffectDefinition {
        init {
            require(multiplier >= Ratio.ONE && multiplier <= Ratio.ofUnits(20_000L)) {
                "Conditional enemy damage multiplier must be within [100%, 200%]"
            }
        }
    }
}
