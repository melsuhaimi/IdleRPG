package com.idlerpg.game.domain.model.quest

import com.idlerpg.game.core.id.ContentId

/** Chronicle-resettable aggregate of event-driven quest progress. */
data class QuestState(
    val progressByQuestId: Map<ContentId, QuestProgressState> = emptyMap()
) {
    fun progressFor(questId: ContentId): QuestProgressState =
        progressByQuestId[questId] ?: QuestProgressState()
}
