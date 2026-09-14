package com.idlerpg.game.domain.definition.combat

import com.idlerpg.game.core.id.ContentId
import com.idlerpg.game.core.number.GameNumber
import com.idlerpg.game.core.time.GameDuration
import com.idlerpg.game.domain.definition.Affinity

/** Deterministic stacking behavior for one authored status definition. */
enum class StatusStackingPolicy {
    /** Re-applying the status preserves one stack and refreshes its duration/tick schedule. */
    REFRESH_DURATION,

    /** Re-applying increases stacks up to maxStacks and refreshes duration/tick schedule. */
    STACK_AND_REFRESH
}

/** Bounded stat modifiers supplied by an active status effect. */
sealed interface StatusModifierDefinition {
    data class FlatAttackPower(
        val amountPerStack: GameNumber
    ) : StatusModifierDefinition

    data class FlatArmor(
        val amountPerStack: GameNumber
    ) : StatusModifierDefinition

    data class DamageTakenBonus(
        val ratioPerStack: com.idlerpg.game.core.number.Ratio
    ) : StatusModifierDefinition {
        init {
            require(ratioPerStack > com.idlerpg.game.core.number.Ratio.ZERO)
            require(ratioPerStack <= com.idlerpg.game.core.number.Ratio.ONE)
        }
    }

    /** Multiplies the player's canonical action interval while this status is active. */
    data class ActionIntervalMultiplier(
        val multiplierPerStack: com.idlerpg.game.core.number.Ratio
    ) : StatusModifierDefinition {
        init {
            require(multiplierPerStack >= com.idlerpg.game.core.number.Ratio.ONE)
            require(multiplierPerStack <= com.idlerpg.game.core.number.Ratio.ofUnits(20_000L))
        }
    }

    /** Multiplies incoming enemy damage before the player's armor mitigation. */
    data class IncomingDamageMultiplier(
        val multiplierPerStack: com.idlerpg.game.core.number.Ratio
    ) : StatusModifierDefinition {
        init {
            require(multiplierPerStack >= com.idlerpg.game.core.number.Ratio.ONE)
            require(multiplierPerStack <= com.idlerpg.game.core.number.Ratio.ofUnits(20_000L))
        }
    }
}

/**
 * Authored status-effect content.
 *
 * Runtime timing, stack count, potency and source identity live in StatusEffectState.
 * Definition effects are interpreted by StatusEffectSystem at deterministic simulation
 * timestamps; no wall-clock API or coroutine timer participates in status resolution.
 */
data class StatusEffectDefinition(
    val id: ContentId,
    val displayName: String,
    val duration: GameDuration,
    val stackingPolicy: StatusStackingPolicy = StatusStackingPolicy.REFRESH_DURATION,
    val maximumStacks: Int = 1,
    val periodicInterval: GameDuration? = null,
    val modifiers: List<StatusModifierDefinition> = emptyList(),
    val periodicEffects: List<EffectSpec> = emptyList(),
    val expireEffects: List<EffectSpec> = emptyList(),
    val affinityTags: Set<Affinity> = emptySet()
) {
    init {
        require(displayName.isNotBlank()) {
            "StatusEffectDefinition.displayName cannot be blank for $id"
        }
        require(duration > GameDuration.ZERO) {
            "StatusEffectDefinition.duration must be > 0 for $id"
        }
        require(maximumStacks in 1..100) {
            "StatusEffectDefinition.maximumStacks must be between 1 and 100 for $id"
        }
        if (periodicInterval != null) {
            require(periodicInterval > GameDuration.ZERO) {
                "StatusEffectDefinition.periodicInterval must be > 0 for $id"
            }
            require(periodicEffects.isNotEmpty()) {
                "Periodic status $id requires periodicEffects"
            }
        } else {
            require(periodicEffects.isEmpty()) {
                "Status $id cannot declare periodicEffects without periodicInterval"
            }
        }
    }
}
