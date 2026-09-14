package com.idlerpg.game.data.content

import com.idlerpg.game.core.id.ContentId
import com.idlerpg.game.core.number.GameNumber
import com.idlerpg.game.domain.definition.CurrencyId
import com.idlerpg.game.domain.definition.quest.QuestDefinition
import com.idlerpg.game.domain.definition.quest.QuestObjectiveDefinition
import com.idlerpg.game.domain.definition.quest.QuestRewardDefinition

/** Run objectives teach combat roles, equipment and affinity combinations. */
object TrainingHollowQuestContent {
    fun questId(name: String) = ContentId("quest.training.$name")
    fun objectiveId(name: String) = ContentId("objective.quest.training.$name")

    val quests = listOf(
        QuestDefinition(questId("first_resonance"), "First Combo",
            listOf(QuestObjectiveDefinition.TriggerConvergence(objectiveId("first_resonance"))),
            gold(50L), requiredPlayerLevel = 3L),
        QuestDefinition(questId("break_protector"), "Break the Protector",
            listOf(QuestObjectiveDefinition.KillEnemy(objectiveId("break_protector"), DefaultGameContent.HOLLOW_BULWARK_ID)),
            gold(75L), requiredPlayerLevel = 5L),
        QuestDefinition(questId("silence_seer"), "Silence the Seer",
            listOf(QuestObjectiveDefinition.KillEnemy(objectiveId("silence_seer"), DefaultGameContent.ARCANE_SEER_ID)),
            gold(100L), requiredPlayerLevel = 8L),
        QuestDefinition(questId("worthy_find"), "A Worthy Find",
            listOf(QuestObjectiveDefinition.AcquireItem(objectiveId("worthy_find"))), gold(50L)),
        QuestDefinition(questId("into_depths"), "Into the Depths",
            listOf(QuestObjectiveDefinition.ClearEncounter(objectiveId("into_depths"), TrainingHollowWorldContent.stageId(10))), gold(150L)),
        QuestDefinition(questId("end_warden"), "End the Warden",
            listOf(QuestObjectiveDefinition.ClearEncounter(objectiveId("end_warden"), TrainingHollowWorldContent.stageId(30))), gold(300L)),
        QuestDefinition(questId("hollow_patrol"), "Hollow Patrol",
            listOf(QuestObjectiveDefinition.KillEnemy(objectiveId("hollow_patrol"), requiredCount = GameNumber.of(25L))),
            gold(30L), repeatable = true),
        QuestDefinition(questId("steady_expedition"), "Steady Adventure",
            listOf(QuestObjectiveDefinition.ClearEncounter(objectiveId("steady_expedition"), requiredCount = GameNumber.of(10L))),
            gold(40L), repeatable = true),
        QuestDefinition(questId("resonance_practice"), "Combo Practice",
            listOf(QuestObjectiveDefinition.TriggerConvergence(objectiveId("resonance_practice"), requiredCount = GameNumber.of(5L))),
            gold(50L), requiredPlayerLevel = 3L, repeatable = true)
    )

    private fun gold(amount: Long) = QuestRewardDefinition(currencies = mapOf(CurrencyId.GOLD to GameNumber.of(amount)))
}
