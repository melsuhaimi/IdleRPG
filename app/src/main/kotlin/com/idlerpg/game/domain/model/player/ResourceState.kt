package com.idlerpg.game.domain.model.player

import com.idlerpg.game.core.id.ContentId
import com.idlerpg.game.core.number.GameNumber

/**
 * Run-level combat resources other than currencies.
 *
 * Resource kinds are referenced by stable ContentId so later skill foundations can add
 * energy, mana, rage, or other resources without adding one field per resource type.
 */
data class ResourceState(
    val amounts: Map<ContentId, GameNumber> = emptyMap()
)
