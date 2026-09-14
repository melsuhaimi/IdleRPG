package com.idlerpg.game.presentation.query

import com.idlerpg.game.core.id.ContentId
import com.idlerpg.game.core.number.GameNumber
import com.idlerpg.game.core.number.Ratio
import com.idlerpg.game.data.content.ContentRegistry
import com.idlerpg.game.domain.model.GameState
import com.idlerpg.game.domain.system.adaptation.AdaptationRewardSystem

/** World-specific read facade. It delegates gameplay-derived values to backend owners. */
interface WorldReadQueries {
    fun nextAdaptationThreshold(regionId: ContentId, currentTier: Int): GameNumber?
    fun activeEnemyRewardMultiplier(state: GameState): Ratio
}

class DefaultWorldReadQueries(
    private val contentRegistry: ContentRegistry
) : WorldReadQueries {
    override fun nextAdaptationThreshold(
        regionId: ContentId,
        currentTier: Int
    ): GameNumber? {
        require(currentTier >= 0) { "currentTier cannot be negative" }
        val region = contentRegistry.region(regionId)
        val thresholdsId = region.adaptationThresholdDefinitionId ?: return null
        return contentRegistry
            .adaptationThreshold(thresholdsId)
            .minimumPressureByTier[currentTier + 1]
    }

    override fun activeEnemyRewardMultiplier(state: GameState): Ratio {
        val enemy = state.run.combat.enemies.firstOrNull()
        val region = state.run.world.activeRegionId?.let(contentRegistry::regionOrNull)
        return AdaptationRewardSystem.rewardMultiplier(
            enemy = enemy,
            regionDefinition = region,
            contentRegistry = contentRegistry
        )
    }
}
