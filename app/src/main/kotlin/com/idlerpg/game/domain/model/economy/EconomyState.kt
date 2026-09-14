package com.idlerpg.game.domain.model.economy

/** Run-level economy aggregate. */
data class EconomyState(
    val wallet: CurrencyWallet = CurrencyWallet(),
    val upgrades: UpgradeProgressState = UpgradeProgressState()
)
