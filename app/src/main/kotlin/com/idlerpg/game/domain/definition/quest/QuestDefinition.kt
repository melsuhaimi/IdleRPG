package com.idlerpg.game.domain.definition.quest

import com.idlerpg.game.core.id.ContentId
import com.idlerpg.game.core.number.GameNumber
import com.idlerpg.game.domain.definition.CurrencyId

/**
 * Bounded event-objective vocabulary for run-scoped quests.
 *
 * Foundation 14 intentionally supports only completed domain facts that already exist in
 * the event model. Objective progress is never advanced by UI polling.
 */
sealed interface QuestObjectiveDefinition {
    val id: ContentId
    val requiredCount: GameNumber

    data class KillEnemy(
        override val id: ContentId,
        val enemyDefinitionId: ContentId? = null,
        override val requiredCount: GameNumber = GameNumber.ONE
    ) : QuestObjectiveDefinition

    data class ClearEncounter(
        override val id: ContentId,
        val encounterDefinitionId: ContentId? = null,
        override val requiredCount: GameNumber = GameNumber.ONE
    ) : QuestObjectiveDefinition

    data class TriggerConvergence(
        override val id: ContentId,
        val convergenceId: ContentId? = null,
        override val requiredCount: GameNumber = GameNumber.ONE
    ) : QuestObjectiveDefinition

    data class AcquireItem(
        override val id: ContentId,
        val itemDefinitionId: ContentId? = null,
        override val requiredCount: GameNumber = GameNumber.ONE
    ) : QuestObjectiveDefinition
}

/**
 * Authored reward granted atomically when a quest completes.
 *
 * Foundation 14 reuses the existing RewardSystem vocabulary rather than creating a
 * second currency/XP/loot transaction implementation.
 */
data class QuestRewardDefinition(
    val currencies: Map<CurrencyId, GameNumber> = emptyMap(),
    val experience: GameNumber = GameNumber.ZERO,
    val lootTableIds: List<ContentId> = emptyList()
)

/**
 * Static run-scoped quest definition.
 *
 * Repeatable contracts restart their objectives immediately on completion. Unclaimed
 * currency/XP rewards accumulate and are collected together with one explicit claim.
 */
data class QuestDefinition(
    val id: ContentId,
    val displayName: String,
    val objectives: List<QuestObjectiveDefinition>,
    val reward: QuestRewardDefinition = QuestRewardDefinition(),
    val requiredPlayerLevel: Long = 1L,
    val requiredFeatureIds: Set<ContentId> = emptySet(),
    val repeatable: Boolean = false
) {
    init {
        require(displayName.isNotBlank()) { "Quest displayName cannot be blank for $id" }
        require(objectives.isNotEmpty()) { "Quest $id must contain at least one objective" }
        require(requiredPlayerLevel > 0L) {
            "Quest requiredPlayerLevel must be positive for $id"
        }
        require(objectives.map { it.id }.size == objectives.map { it.id }.toSet().size) {
            "Quest $id contains duplicate objective IDs"
        }
        require(objectives.all { it.requiredCount > GameNumber.ZERO }) {
            "Quest $id objective counts must be > 0"
        }
    }
}
