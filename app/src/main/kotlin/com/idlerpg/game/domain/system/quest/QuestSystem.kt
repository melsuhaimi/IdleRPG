package com.idlerpg.game.domain.system.quest

import com.idlerpg.game.core.number.GameNumber
import com.idlerpg.game.domain.definition.quest.QuestDefinition
import com.idlerpg.game.domain.definition.quest.QuestObjectiveDefinition
import com.idlerpg.game.domain.engine.EngineContext
import com.idlerpg.game.domain.event.ConvergenceTriggered
import com.idlerpg.game.domain.event.EncounterCleared
import com.idlerpg.game.domain.event.EnemyKilled
import com.idlerpg.game.domain.event.GameEvent
import com.idlerpg.game.domain.event.ItemAdded
import com.idlerpg.game.domain.event.ItemSentToOverflow
import com.idlerpg.game.domain.event.QuestCompleted
import com.idlerpg.game.domain.model.GameState
import com.idlerpg.game.domain.model.quest.QuestProgressState

/** Atomic reaction of run-quest state to one completed domain event. */
data class QuestReactionResult(
    val state: GameState,
    val events: List<GameEvent> = emptyList()
)

/** Event-driven run-quest tracker. Completion and reward collection are separate. */
object QuestSystem {

    fun react(
        state: GameState,
        event: GameEvent,
        context: EngineContext
    ): QuestReactionResult {
        var transitioned = state
        val generatedEvents = mutableListOf<GameEvent>()

        context.contentRegistry.allQuests()
            .sortedBy { it.id }
            .forEach { definition ->
                if (!isEligible(transitioned, definition)) return@forEach

                val current = transitioned.run.quests.progressFor(definition.id)
                if (!definition.repeatable && current.completionCount > GameNumber.ZERO) {
                    return@forEach
                }

                val updated = applyEvent(current, definition, event)
                if (updated == current) return@forEach

                val complete = isComplete(updated, definition)
                val committed = if (complete) {
                    updated.copy(
                        completionCount = updated.completionCount + GameNumber.ONE,
                        progressByObjectiveId = if (definition.repeatable) emptyMap() else updated.progressByObjectiveId
                    )
                } else updated

                transitioned = transitioned.copy(
                    run = transitioned.run.copy(
                        quests = transitioned.run.quests.copy(
                            progressByQuestId = transitioned.run.quests.progressByQuestId +
                                (definition.id to committed)
                        )
                    )
                )

                if (complete) {
                    generatedEvents += QuestCompleted(definition.id, committed.completionCount)
                }
            }

        return QuestReactionResult(transitioned, generatedEvents)
    }

    fun isEligible(state: GameState, definition: QuestDefinition): Boolean {
        if (state.run.progression.playerLevel.level < definition.requiredPlayerLevel) return false
        val unlocked = state.run.progression.featureUnlocks.unlockedFeatureIds +
            state.meta.persistentFeatureUnlocks.unlockedFeatureIds
        return definition.requiredFeatureIds.all { it in unlocked }
    }

    private fun applyEvent(
        progress: QuestProgressState,
        definition: QuestDefinition,
        event: GameEvent
    ): QuestProgressState {
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

    private fun isComplete(progress: QuestProgressState, definition: QuestDefinition): Boolean =
        definition.objectives.all { objective ->
            (progress.progressByObjectiveId[objective.id] ?: GameNumber.ZERO) >= objective.requiredCount
        }

    private fun matches(objective: QuestObjectiveDefinition, event: GameEvent): Boolean =
        when (objective) {
            is QuestObjectiveDefinition.KillEnemy -> event is EnemyKilled &&
                (objective.enemyDefinitionId == null || objective.enemyDefinitionId == event.enemyDefinitionId)
            is QuestObjectiveDefinition.ClearEncounter -> event is EncounterCleared &&
                (objective.encounterDefinitionId == null || objective.encounterDefinitionId == event.encounterDefinitionId)
            is QuestObjectiveDefinition.TriggerConvergence -> event is ConvergenceTriggered &&
                (objective.convergenceId == null || objective.convergenceId == event.convergenceId)
            is QuestObjectiveDefinition.AcquireItem -> when (event) {
                is ItemAdded -> objective.itemDefinitionId == null ||
                    objective.itemDefinitionId == event.itemDefinitionId
                is ItemSentToOverflow -> objective.itemDefinitionId == null ||
                    objective.itemDefinitionId == event.itemDefinitionId
                else -> false
            }
        }
}
