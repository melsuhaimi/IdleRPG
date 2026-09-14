package com.idlerpg.game.core.config

/** Root backend configuration object. */
data class GameConfig(
    val contentVersion: String = DEFAULT_CONTENT_VERSION,
    val balance: BalanceConfig = BalanceConfig(),
    val simulation: SimulationConfig = SimulationConfig(),
    val developmentFeatureFlags: Set<String> = emptySet()
) {
    init {
        require(contentVersion.isNotBlank()) { "contentVersion cannot be blank" }
        require(developmentFeatureFlags.none { it.isBlank() }) {
            "developmentFeatureFlags cannot contain blank values"
        }
    }

    companion object {
        const val DEFAULT_CONTENT_VERSION: String = "foundation-1"
    }
}
