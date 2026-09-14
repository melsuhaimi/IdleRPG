package com.idlerpg.game.domain.system.economy

import com.idlerpg.game.core.number.GameNumber
import com.idlerpg.game.domain.definition.CurrencyId
import com.idlerpg.game.domain.model.economy.CurrencyWallet
import com.idlerpg.game.domain.model.economy.EconomyState

/**
 * Atomic currency balance operations.
 *
 * This system changes economy state only. Higher-level systems own semantic events such
 * as CurrencyGranted, CurrencySpent, and UpgradePurchased.
 */
object TransactionSystem {

    fun balance(
        economy: EconomyState,
        currencyId: CurrencyId
    ): GameNumber =
        economy.wallet.amountsByCurrencyId[currencyId]
            ?: GameNumber.ZERO

    fun canAfford(
        economy: EconomyState,
        currencyId: CurrencyId,
        amount: GameNumber
    ): Boolean =
        balance(economy, currencyId) >= amount

    fun grant(
        economy: EconomyState,
        currencyId: CurrencyId,
        amount: GameNumber
    ): EconomyState {
        if (amount == GameNumber.ZERO) {
            return economy
        }

        val newBalance = balance(economy, currencyId) + amount
        return economy.copy(
            wallet = CurrencyWallet(
                amountsByCurrencyId =
                    (economy.wallet.amountsByCurrencyId +
                        (currencyId to newBalance))
                        .toSortedMap()
            )
        )
    }

    /**
     * Return updated economy when affordable, otherwise null.
     *
     * No partial mutation occurs when the balance is insufficient.
     */
    fun spend(
        economy: EconomyState,
        currencyId: CurrencyId,
        amount: GameNumber
    ): EconomyState? {
        val current = balance(economy, currencyId)
        if (current < amount) {
            return null
        }

        val newBalance = current - amount
        return economy.copy(
            wallet = CurrencyWallet(
                amountsByCurrencyId =
                    (economy.wallet.amountsByCurrencyId +
                        (currencyId to newBalance))
                        .toSortedMap()
            )
        )
    }
}
