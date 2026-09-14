package com.idlerpg.game.domain.system.achievement

import com.idlerpg.game.core.number.GameNumber
import com.idlerpg.game.domain.definition.achievement.AchievementDefinition
import com.idlerpg.game.domain.definition.achievement.AchievementObjectiveDefinition
import com.idlerpg.game.domain.engine.EngineContext
import com.idlerpg.game.domain.event.AchievementCompleted
import com.idlerpg.game.domain.event.ConvergenceTriggered
import com.idlerpg.game.domain.event.EncounterCleared
import com.idlerpg.game.domain.event.EnemyKilled
import com.idlerpg.game.domain.event.GameEvent
import com.idlerpg.game.domain.event.ItemAdded
import com.idlerpg.game.domain.event.ItemSentToOverflow
import com.idlerpg.game.domain.model.GameState
import com.idlerpg.game.domain.model.achievement.AchievementProgressState

/** Atomic reaction of persistent achievement state to one completed domain event. */
data class AchievementReactionResult(
    val state: GameState,
    val events: List<GameEvent> = emptyList()
)

/** Persistent event-driven achievement tracker. Completion and reward collection are separate. */
object AchievementSystem {

    fun react(state: GameState, event: GameEvent, context: EngineContext): AchievementReactionResult {
        var transitioned = state
        val generatedEvents = mutableListOf<GameEvent>()
        context.contentRegistry.allAchievements().sortedBy { it.id }.forEach { definition ->
            if (!isEligible(transitioned, definition)) return@forEach
            val current = transitioned.meta.achievements.progressFor(definition.id)
            if (current.completed) return@forEach
            val updated = applyEvent(current, definition, event)
            if (updated == current) return@forEach
            val complete = isComplete(updated, definition)
            val committed = updated.copy(completed = complete)
            transitioned = transitioned.copy(
                meta = transitioned.meta.copy(
                    achievements = transitioned.meta.achievements.copy(
                        progressByAchievementId = transitioned.meta.achievements.progressByAchievementId +
                            (definition.id to committed)
                    )
                )
            )
            if (complete) generatedEvents += AchievementCompleted(definition.id)
        }
        return AchievementReactionResult(transitioned, generatedEvents)
    }

    fun isEligible(state: GameState, definition: AchievementDefinition): Boolean {
        val unlocked = state.run.progression.featureUnlocks.unlockedFeatureIds +
            state.meta.persistentFeatureUnlocks.unlockedFeatureIds
        return definition.requiredFeatureIds.all { it in unlocked }
    }

    private fun applyEvent(
        progress: AchievementProgressState,
        definition: AchievementDefinition,
        event: GameEvent
    ): AchievementProgressState {
        var updated = progress.progressByObjectiveId
        definition.objectives.sortedBy { it.id }.forEach { objective ->
            if (!matches(objective, event)) return@forEach
            val previous = updated[objective.id] ?: GameNumber.ZERO
            val next = previous + GameNumber.ONE
            val bounded = if (next > objective.requiredCount) objective.requiredCount else next
            updated = updated + (objective.id to bounded)
        }
        return if (updated == progress.progressByObjectiveId) progress
        else progress.copy(progressByObjectiveId = updated)
    }

    private fun isComplete(progress: AchievementProgressState, definition: AchievementDefinition): Boolean =
        definition.objectives.all { objective ->
            (progress.progressByObjectiveId[objective.id] ?: GameNumber.ZERO) >= objective.requiredCount
        }

    private fun matches(objective: AchievementObjectiveDefinition, event: GameEvent): Boolean =
        when (objective) {
            is AchievementObjectiveDefinition.KillEnemy -> event is EnemyKilled &&
                (objective.enemyDefinitionId == null || objective.enemyDefinitionId == event.enemyDefinitionId)
            is AchievementObjectiveDefinition.ClearEncounter -> event is EncounterCleared &&
                (objective.encounterDefinitionId == null || objective.encounterDefinitionId == event.encounterDefinitionId)
            is AchievementObjectiveDefinition.TriggerConvergence -> event is ConvergenceTriggered &&
                (objective.convergenceId == null || objective.convergenceId == event.convergenceId)
            is AchievementObjectiveDefinition.AcquireItem -> when (event) {
                is ItemAdded -> objective.itemDefinitionId == null ||
                    objective.itemDefinitionId == event.itemDefinitionId
                is ItemSentToOverflow -> objective.itemDefinitionId == null ||
                    objective.itemDefinitionId == event.itemDefinitionId
                else -> false
            }
        }
}
