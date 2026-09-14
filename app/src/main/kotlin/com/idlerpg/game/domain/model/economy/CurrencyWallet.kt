package com.idlerpg.game.domain.model.economy

import com.idlerpg.game.core.number.GameNumber
import com.idlerpg.game.domain.definition.CurrencyId

/**
 * Stable run-level currency ownership map.
 *
 * Foundation 6 replaces the temporary Foundation 2 ContentId key with the dedicated
 * typed CurrencyId vocabulary while preserving stable ContentId-backed identity.
 */
data class CurrencyWallet(
    val amountsByCurrencyId: Map<CurrencyId, GameNumber> = emptyMap()
)
