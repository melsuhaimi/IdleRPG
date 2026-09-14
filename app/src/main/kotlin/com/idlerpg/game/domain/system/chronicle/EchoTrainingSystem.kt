package com.idlerpg.game.domain.system.chronicle

import com.idlerpg.game.core.id.ContentId
import com.idlerpg.game.data.content.ContentRegistry
import com.idlerpg.game.data.content.EchoTrainingContent
import com.idlerpg.game.domain.model.GameState
import com.idlerpg.game.domain.model.MetaState
import com.idlerpg.game.domain.model.economy.UpgradeProgressState

/** Paid training is derived from persistent purchases; run levels keep their existing save format. */
object EchoTrainingSystem {
    fun startingLevels(meta: MetaState): Map<ContentId, Long> = EchoTrainingContent.upgradeIds
        .filter { EchoTrainingContent.offerId(it) in meta.echoes.purchasedOfferIds }
        .associateWith { EchoTrainingContent.STARTING_LEVEL }

    fun applyPurchase(state: GameState, offerId: ContentId, registry: ContentRegistry): GameState {
        val id = EchoTrainingContent.upgradeIds.firstOrNull { EchoTrainingContent.offerId(it) == offerId } ?: return state
        val levels = state.run.economy.upgrades.levelByUpgradeId
        val current = levels[id] ?: 0L
        val maximum = registry.upgrade(id).maximumPurchasedLevel
        val gain = minOf(EchoTrainingContent.STARTING_LEVEL, (maximum - current).coerceAtLeast(0L))
        return state.copy(run = state.run.copy(economy = state.run.economy.copy(
            upgrades = UpgradeProgressState(levels + (id to (current + gain)))
        )))
    }
}
