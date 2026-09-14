package com.idlerpg.game.domain.definition.achievement

import com.idlerpg.game.core.id.ContentId
import com.idlerpg.game.core.number.GameNumber
import com.idlerpg.game.domain.definition.CurrencyId

/**
 * Bounded event-objective vocabulary for persistent achievements.
 *
 * Run-only challenge semantics are intentionally not overloaded onto achievements.
 */
sealed interface AchievementObjectiveDefinition {
    val id: ContentId
    val requiredCount: GameNumber

    data class KillEnemy(
        override val id: ContentId,
        val enemyDefinitionId: ContentId? = null,
        override val requiredCount: GameNumber = GameNumber.ONE
    ) : AchievementObjectiveDefinition

    data class ClearEncounter(
        override val id: ContentId,
        val encounterDefinitionId: ContentId? = null,
        override val requiredCount: GameNumber = GameNumber.ONE
    ) : AchievementObjectiveDefinition

    data class TriggerConvergence(
        override val id: ContentId,
        val convergenceId: ContentId? = null,
        override val requiredCount: GameNumber = GameNumber.ONE
    ) : AchievementObjectiveDefinition

    data class AcquireItem(
        override val id: ContentId,
        val itemDefinitionId: ContentId? = null,
        override val requiredCount: GameNumber = GameNumber.ONE
    ) : AchievementObjectiveDefinition
}

/** Authored reward granted atomically on first persistent achievement completion. */
data class AchievementRewardDefinition(
    val currencies: Map<CurrencyId, GameNumber> = emptyMap(),
    val experience: GameNumber = GameNumber.ZERO,
    val lootTableIds: List<ContentId> = emptyList()
)

/**
 * Static persistent achievement definition.
 *
 * Completion is stored in MetaState. A future run-only challenge system must use a
 * separate model rather than weakening that reset-scope invariant.
 */
data class AchievementDefinition(
    val id: ContentId,
    val displayName: String,
    val objectives: List<AchievementObjectiveDefinition>,
    val reward: AchievementRewardDefinition = AchievementRewardDefinition(),
    val requiredFeatureIds: Set<ContentId> = emptySet()
) {
    init {
        require(displayName.isNotBlank()) {
            "Achievement displayName cannot be blank for $id"
        }
        require(objectives.isNotEmpty()) {
            "Achievement $id must contain at least one objective"
        }
        require(objectives.map { it.id }.size == objectives.map { it.id }.toSet().size) {
            "Achievement $id contains duplicate objective IDs"
        }
        require(objectives.all { it.requiredCount > GameNumber.ZERO }) {
            "Achievement $id objective counts must be > 0"
        }
    }
}
