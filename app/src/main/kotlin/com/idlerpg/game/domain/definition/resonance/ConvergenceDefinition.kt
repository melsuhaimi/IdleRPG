package com.idlerpg.game.domain.definition.resonance

import com.idlerpg.game.core.id.ContentId
import com.idlerpg.game.core.number.GameNumber
import com.idlerpg.game.core.time.GameDuration
import com.idlerpg.game.domain.definition.Affinity
import com.idlerpg.game.domain.definition.combat.EffectSpec

/**
 * Static authored Convergence content.
 *
 * Matching order is not encoded by collection insertion order. ConvergenceSystem applies
 * the project contract: longest matching pattern, then higher [priority], then stable ID.
 */
data class ConvergenceDefinition(
    val id: ContentId,
    val pattern: ResonancePatternDefinition,
    val minimumChargeByAffinity: Map<Affinity, GameNumber> = emptyMap(),
    val consumePolicy: ResonanceConsumePolicy = ResonanceConsumePolicy.NONE,
    val priority: Int = 0,
    val cooldown: GameDuration = GameDuration.ZERO,
    val maxTriggersPerEncounter: Long? = null,
    val effects: List<EffectSpec>,
    val discoverOnFirstTrigger: Boolean = true
) {
    init {
        require(minimumChargeByAffinity.values.all { it > GameNumber.ZERO }) {
            "ConvergenceDefinition minimum charge values must be > 0 for $id"
        }
        require(maxTriggersPerEncounter == null || maxTriggersPerEncounter > 0L) {
            "ConvergenceDefinition.maxTriggersPerEncounter must be positive when set for $id"
        }
        require(effects.isNotEmpty()) {
            "ConvergenceDefinition.effects cannot be empty for $id"
        }
        if (consumePolicy == ResonanceConsumePolicy.REQUIRED_CHARGE) {
            require(minimumChargeByAffinity.isNotEmpty()) {
                "REQUIRED_CHARGE needs minimumChargeByAffinity for $id"
            }
        }
    }
}
