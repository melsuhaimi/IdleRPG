package com.idlerpg.game.domain.system.reward

import com.idlerpg.game.core.id.ContentId
import com.idlerpg.game.core.number.GameMath
import com.idlerpg.game.core.number.Ratio
import com.idlerpg.game.data.content.ContentRegistry
import com.idlerpg.game.domain.definition.CurrencyId
import com.idlerpg.game.domain.model.GameState
import com.idlerpg.game.domain.model.combat.EnemyState
import com.idlerpg.game.domain.system.adaptation.AdaptationRewardSystem

/**
 * Derived reward multiplier boundary.
 *
 * Foundation 10 composes the active region modifier with mutation difficulty compensation.
 * The result is derived and is not stored as canonical state.
 */
object RewardMultiplierSystem {

    @Suppress("UNUSED_PARAMETER")
    fun currencyMultiplier(
        state: GameState,
        currencyId: CurrencyId,
        contentRegistry: ContentRegistry,
        sourceId: ContentId? = null,
        defeatedEnemy: EnemyState? = null
    ): Ratio {
        val regionId = state.run.world.activeRegionId
        val regionDefinition =
            regionId?.let(contentRegistry::regionOrNull)

        val regionMultiplier =
            regionDefinition?.rewardMultiplier ?: Ratio.ONE
        val adaptationMultiplier = AdaptationRewardSystem.rewardMultiplier(
            enemy = defeatedEnemy,
            regionDefinition = regionDefinition,
            contentRegistry = contentRegistry
        )

        return combine(
            first = regionMultiplier,
            second = adaptationMultiplier
        )
    }

    private fun combine(
        first: Ratio,
        second: Ratio
    ): Ratio = GameMath.multiplyRatios(first, second)
}
