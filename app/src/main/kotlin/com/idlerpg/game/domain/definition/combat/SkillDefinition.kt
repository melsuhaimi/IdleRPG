package com.idlerpg.game.domain.definition.combat

import com.idlerpg.game.core.id.ContentId
import com.idlerpg.game.core.number.GameNumber
import com.idlerpg.game.core.number.Ratio
import com.idlerpg.game.core.time.GameDuration
import com.idlerpg.game.domain.definition.Affinity
import com.idlerpg.game.domain.definition.resonance.ResonanceEmissionDefinition

/** Initial deterministic targeting vocabulary for player skills. */
enum class SkillTargetingRule {
    PRIMARY_ENEMY,
    LOWEST_HEALTH_ENEMY,
    HIGHEST_HEALTH_ENEMY,
    PROTECTOR_FIRST,
    CASTER_OR_SUPPORT_FIRST,
    SELF
}

/**
 * Authored skill contract through Foundation 12.
 *
 * Cooldown/recovery/resource legality is interpreted by SkillValidationSystem and
 * SkillSystem. Skill selection remains Doctrine-owned. A skill can optionally require an
 * explicit loadout entry and/or a stable feature unlock without embedding progression
 * formulas in combat code.
 */
data class SkillDefinition(
    val id: ContentId,
    val cooldown: GameDuration = GameDuration.ZERO,
    val recovery: GameDuration = GameDuration.ofMillis(1_000L),
    val targetingRule: SkillTargetingRule = SkillTargetingRule.PRIMARY_ENEMY,
    val resourceCosts: Map<ContentId, GameNumber> = emptyMap(),
    val effects: List<EffectSpec>,
    val affinityTags: Set<Affinity> = emptySet(),
    val resonanceEmissions: List<ResonanceEmissionDefinition> = emptyList(),
    val requiresEquipped: Boolean = true,
    val requiredFeatureId: ContentId? = null,
    /** Additive skill-power growth per skill rank after rank one. */
    val powerGrowthPerPlayerLevel: Ratio = Ratio.ofUnits(75L),
    /** Additive healing growth per skill rank after rank one. */
    val healingGrowthPerPlayerLevel: Ratio = Ratio.ofUnits(60L),
    /** Skill rank is a separate run-scoped investment and stops at this authored cap. */
    val maxRank: Long? = 100L
) {
    init {
        require(recovery > GameDuration.ZERO) {
            "SkillDefinition.recovery must be > 0 for $id"
        }
        require(effects.isNotEmpty()) {
            "SkillDefinition.effects cannot be empty for $id"
        }
        require(resourceCosts.values.all { it > GameNumber.ZERO }) {
            "SkillDefinition.resourceCosts must contain only positive amounts for $id"
        }
        require(powerGrowthPerPlayerLevel >= Ratio.ZERO) {
            "SkillDefinition.powerGrowthPerPlayerLevel cannot be negative for $id"
        }
        require(healingGrowthPerPlayerLevel >= Ratio.ZERO) {
            "SkillDefinition.healingGrowthPerPlayerLevel cannot be negative for $id"
        }
        require(maxRank == null || maxRank >= 1L) {
            "SkillDefinition.maxRank must be >= 1 when present for $id"
        }
    }
}
