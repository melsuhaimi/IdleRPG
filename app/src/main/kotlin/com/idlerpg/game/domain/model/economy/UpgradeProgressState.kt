package com.idlerpg.game.domain.model.economy

import com.idlerpg.game.core.id.ContentId

/** Purchased run-upgrade levels keyed by stable upgrade definition ID. */
data class UpgradeProgressState(
    val levelByUpgradeId: Map<ContentId, Long> = emptyMap()
) {
    init {
        require(levelByUpgradeId.values.all { it >= 0L }) {
            "Upgrade levels cannot be negative"
        }
    }
}
