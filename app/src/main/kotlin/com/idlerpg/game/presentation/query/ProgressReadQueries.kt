package com.idlerpg.game.presentation.query

import com.idlerpg.game.core.id.ContentId
import com.idlerpg.game.core.number.GameNumber
import com.idlerpg.game.data.content.ContentRegistry
import com.idlerpg.game.domain.model.GameState
import com.idlerpg.game.domain.system.achievement.AchievementSystem
import com.idlerpg.game.domain.system.chronicle.ChronicleSystem
import com.idlerpg.game.domain.system.chronicle.EchoOfferSystem
import com.idlerpg.game.domain.system.quest.QuestSystem

/** Read-only Progress facade over existing backend owners. */
interface ProgressReadQueries {
    fun isQuestEligible(state: GameState, questId: ContentId): Boolean
    fun isAchievementEligible(state: GameState, achievementId: ContentId): Boolean
    fun chronicleTotalNormalClears(state: GameState): GameNumber
    fun isEchoOfferPurchased(state: GameState, offerId: ContentId): Boolean
    fun areEchoOfferPrerequisitesSatisfied(state: GameState, offerId: ContentId): Boolean
}

class DefaultProgressReadQueries(
    private val contentRegistry: ContentRegistry
) : ProgressReadQueries {
    override fun isQuestEligible(state: GameState, questId: ContentId): Boolean {
        val definition = contentRegistry.questOrNull(questId) ?: return false
        return QuestSystem.isEligible(state, definition)
    }

    override fun isAchievementEligible(state: GameState, achievementId: ContentId): Boolean {
        val definition = contentRegistry.achievementOrNull(achievementId) ?: return false
        return AchievementSystem.isEligible(state, definition)
    }

    override fun chronicleTotalNormalClears(state: GameState): GameNumber =
        ChronicleSystem.totalNormalClears(state)

    override fun isEchoOfferPurchased(state: GameState, offerId: ContentId): Boolean {
        val definition = contentRegistry.echoOfferOrNull(offerId) ?: return false
        return EchoOfferSystem.isPurchased(state.meta, definition)
    }

    override fun areEchoOfferPrerequisitesSatisfied(
        state: GameState,
        offerId: ContentId
    ): Boolean {
        val definition = contentRegistry.echoOfferOrNull(offerId) ?: return false
        return EchoOfferSystem.prerequisitesSatisfied(state.meta, definition)
    }
}
