package com.idlerpg.game.domain.system.economy

import com.idlerpg.game.core.id.ContentId
import com.idlerpg.game.core.number.GameNumber
import com.idlerpg.game.domain.command.CommandRejectionCode
import com.idlerpg.game.domain.command.CommandRejectionReason
import com.idlerpg.game.domain.definition.economy.UpgradeDefinition
import com.idlerpg.game.domain.engine.EngineContext
import com.idlerpg.game.domain.event.CurrencySpent
import com.idlerpg.game.domain.event.GameEvent
import com.idlerpg.game.domain.event.UpgradePurchased
import com.idlerpg.game.domain.model.GameState
import com.idlerpg.game.domain.model.economy.UpgradeProgressState

/** Atomic purchase outcome used by EconomySystem. */
sealed interface UpgradePurchaseResult {

    data class Accepted(
        val state: GameState,
        val events: List<GameEvent>,
        val definition: UpgradeDefinition,
        val totalCost: GameNumber,
        val newLevel: Long
    ) : UpgradePurchaseResult

    data class Rejected(
        val reason: CommandRejectionReason
    ) : UpgradePurchaseResult
}

/** Owns upgrade lookup, cost validation, affordability, and level advancement. */
object UpgradeSystem {

    fun currentLevel(
        state: GameState,
        upgradeId: ContentId
    ): Long =
        state.run.economy.upgrades.levelByUpgradeId[upgradeId]
            ?: 0L

    fun currentCost(
        state: GameState,
        upgradeId: ContentId,
        context: EngineContext
    ): GameNumber {
        val upgrade = context.contentRegistry.upgrade(upgradeId)
        val formula = context.contentRegistry.costFormula(upgrade.costFormulaId)

        return CostFormulaSystem.costAtLevel(
            definition = formula,
            level = currentLevel(state, upgradeId)
        )
    }

    fun purchaseCost(
        state: GameState,
        upgradeId: ContentId,
        quantity: Long,
        context: EngineContext
    ): GameNumber? {
        if (quantity <= 0L) return null
        val upgrade = context.contentRegistry.upgradeOrNull(upgradeId) ?: return null
        val oldLevel = currentLevel(state, upgradeId)
        val newLevel = try {
            Math.addExact(oldLevel, quantity)
        } catch (_: ArithmeticException) {
            return null
        }
        if (newLevel > upgrade.maximumPurchasedLevel) return null
        return try {
            CostFormulaSystem.totalCost(
                context.contentRegistry.costFormula(upgrade.costFormulaId),
                oldLevel,
                quantity
            )
        } catch (_: ArithmeticException) {
            null
        }
    }

    /** Exact deterministic MAX quantity; no repeated purchase commands or floating point. */
    fun maximumAffordableQuantity(
        state: GameState,
        upgradeId: ContentId,
        context: EngineContext
    ): Long {
        val upgrade = context.contentRegistry.upgradeOrNull(upgradeId) ?: return 0L
        val oldLevel = currentLevel(state, upgradeId)
        val remaining = upgrade.maximumPurchasedLevel - oldLevel
        if (remaining <= 0L) return 0L
        val balance = state.run.economy.wallet.amountsByCurrencyId[upgrade.currencyId]
            ?: GameNumber.ZERO

        fun affordable(quantity: Long): Boolean {
            if (quantity <= 0L || quantity > remaining) return false
            val cost = purchaseCost(state, upgradeId, quantity, context) ?: return false
            return cost <= balance
        }

        if (!affordable(1L)) return 0L
        var low = 1L
        var high = 1L
        while (high < remaining) {
            val doubled = if (high > Long.MAX_VALUE / 2L) remaining else high * 2L
            val candidate = minOf(remaining, doubled)
            if (!affordable(candidate)) {
                high = candidate
                break
            }
            low = candidate
            high = candidate
        }
        if (low == remaining) return low

        while (low + 1L < high) {
            val mid = low + (high - low) / 2L
            if (affordable(mid)) low = mid else high = mid
        }
        return low
    }

    fun purchase(
        state: GameState,
        upgradeId: ContentId,
        quantity: Long,
        context: EngineContext
    ): UpgradePurchaseResult {
        if (quantity <= 0L) {
            return UpgradePurchaseResult.Rejected(
                CommandRejectionReason(
                    code = CommandRejectionCode.INVALID_ARGUMENT,
                    subjectContentId = upgradeId
                )
            )
        }

        val upgrade = context.contentRegistry.upgradeOrNull(upgradeId)
            ?: return UpgradePurchaseResult.Rejected(
                CommandRejectionReason(
                    code = CommandRejectionCode.UNKNOWN_CONTENT,
                    subjectContentId = upgradeId
                )
            )

        val oldLevel = currentLevel(state, upgradeId)
        val newLevel =
            try {
                Math.addExact(oldLevel, quantity)
            } catch (_: ArithmeticException) {
                return UpgradePurchaseResult.Rejected(
                    CommandRejectionReason(
                        code = CommandRejectionCode.CAPACITY_EXCEEDED,
                        subjectContentId = upgradeId
                    )
                )
            }

        if (newLevel > upgrade.maximumPurchasedLevel) {
            return UpgradePurchaseResult.Rejected(
                CommandRejectionReason(
                    code = CommandRejectionCode.CAPACITY_EXCEEDED,
                    subjectContentId = upgradeId
                )
            )
        }

        val formula = context.contentRegistry.costFormula(upgrade.costFormulaId)
        val totalCost = CostFormulaSystem.totalCost(
            definition = formula,
            startLevel = oldLevel,
            quantity = quantity
        )

        val spentEconomy = TransactionSystem.spend(
            economy = state.run.economy,
            currencyId = upgrade.currencyId,
            amount = totalCost
        ) ?: return UpgradePurchaseResult.Rejected(
            CommandRejectionReason(
                code = CommandRejectionCode.INSUFFICIENT_RESOURCE,
                subjectContentId = upgradeId
            )
        )

        val updatedLevels =
            (spentEconomy.upgrades.levelByUpgradeId +
                (upgradeId to newLevel))
                .toSortedMap()

        val updatedEconomy = spentEconomy.copy(
            upgrades = UpgradeProgressState(
                levelByUpgradeId = updatedLevels
            )
        )

        val updatedState = state.copy(
            run = state.run.copy(
                economy = updatedEconomy
            )
        )

        return UpgradePurchaseResult.Accepted(
            state = updatedState,
            events = listOf(
                CurrencySpent(
                    currencyId = upgrade.currencyId,
                    amount = totalCost,
                    purposeId = upgrade.id
                ),
                UpgradePurchased(
                    upgradeId = upgrade.id,
                    quantity = quantity,
                    newLevel = newLevel,
                    currencyId = upgrade.currencyId,
                    totalCost = totalCost
                )
            ),
            definition = upgrade,
            totalCost = totalCost,
            newLevel = newLevel
        )
    }
}
