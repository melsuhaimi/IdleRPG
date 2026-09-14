package com.idlerpg.game.domain.definition.enemy

import com.idlerpg.game.core.id.ContentId
import com.idlerpg.game.core.number.GameNumber

/** Player-facing tactical identity used by targeting, Doctrine, and encounter diagnosis. */
enum class EnemyRole {
    SWARM,
    ASSASSIN,
    CASTER,
    PROTECTOR,
    DISRUPTOR,
    CONTROLLER,
    PARASITE,
    ADAPTIVE,
    BOSS
}

/**
 * Static authored enemy content.
 *
 * Runtime HP, instance IDs, active mutations, and generated loot instances belong to
 * runtime state rather than this definition. Foundation 13 also authors the enemy XP
 * reward consumed by PlayerProgressionSystem through RewardSystem.
 */
data class EnemyDefinition(
    val id: ContentId,
    val displayName: String,
    val baseHealth: GameNumber,
    val baseGoldReward: GameNumber = GameNumber.ZERO,
    val baseExperienceReward: GameNumber = GameNumber.ZERO,
    val lootTableId: ContentId? = null,
    val attackDefinitionId: ContentId? = null,
    val skillIds: List<ContentId> = emptyList(),
    val adaptationTags: Set<ContentId> = emptySet(),
    val role: EnemyRole = EnemyRole.SWARM,
    /** Base armor is intentionally separate from role growth so tier-zero content stays readable. */
    val baseArmor: GameNumber = GameNumber.ZERO,
    /** Null selects the deterministic role profile; authored content may override it. */
    val scalingProfile: EnemyScalingProfile? = null
) {
    init {
        require(displayName.isNotBlank()) {
            "EnemyDefinition.displayName cannot be blank for $id"
        }
        require(baseHealth > GameNumber.ZERO) {
            "EnemyDefinition.baseHealth must be > 0 for $id"
        }
        require(baseArmor >= GameNumber.ZERO) {
            "EnemyDefinition.baseArmor cannot be negative for $id"
        }
        require(skillIds.size == skillIds.toSet().size) {
            "EnemyDefinition.skillIds cannot contain duplicates for $id"
        }
    }
}
