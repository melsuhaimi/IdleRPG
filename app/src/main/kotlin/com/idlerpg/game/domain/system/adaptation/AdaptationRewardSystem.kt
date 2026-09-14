package com.idlerpg.game.domain.system.adaptation

import com.idlerpg.game.core.number.Ratio
import com.idlerpg.game.data.content.ContentRegistry
import com.idlerpg.game.domain.definition.world.RegionDefinition
import com.idlerpg.game.domain.model.combat.EnemyState

/** Derives reward compensation from the difficulty of active Adaptation mutations. */
object AdaptationRewardSystem {

    fun rewardMultiplier(
        enemy: EnemyState?,
        regionDefinition: RegionDefinition?,
        contentRegistry: ContentRegistry
    ): Ratio {
        if (enemy == null || enemy.activeMutations.isEmpty() || regionDefinition == null) {
            return Ratio.ONE
        }

        val rewardDefinitionId =
            regionDefinition.adaptationRewardDefinitionId ?: return Ratio.ONE
        val definition =
            contentRegistry.adaptationReward(rewardDefinitionId)

        val mutationCount = enemy.activeMutations.size.toLong()
        val bonusUnits = Math.multiplyExact(
            definition.bonusPerActiveMutation.units,
            mutationCount
        )
        val requestedUnits = Math.addExact(
            Ratio.ONE.units,
            Math.addExact(bonusUnits, com.idlerpg.game.core.config.AdaptationCurve.rewardBonusUnits(
                enemy.activeMutations.maxOf { it.adaptationTier }
            ))
        )

        return Ratio.ofUnits(
            minOf(requestedUnits, definition.maximumMultiplier.units)
        )
    }
}
