package com.idlerpg.game.domain.model.inventory

import com.idlerpg.game.core.id.ContentId
import com.idlerpg.game.core.id.InstanceId
import com.idlerpg.game.domain.definition.Rarity

/** One deterministic rolled stat attached to a generated item instance. */
data class RolledAffix(
    val affixId: ContentId,
    val value: Long
) {
    init {
        require(value >= 0L) {
            "RolledAffix.value cannot be negative for $affixId"
        }
    }
}

/**
 * Mutable/generated player-owned item.
 *
 * The authored name/slot/allowed affix pool remain in content definitions. This state
 * stores the main stat, random substat lines, enhancement level, and source facts required
 * to resume the exact item after save/load.
 */
data class ItemInstance(
    val instanceId: InstanceId,
    val definitionId: ContentId,
    val rarity: Rarity,
    val affixes: List<RolledAffix> = emptyList(),
    val sourceDefinitionId: ContentId? = null,
    val mainStat: RolledAffix? = null,
    val enhancementLevel: Int = EnhancementLevel.INITIAL
) {
    init {
        require(affixes.map { it.affixId }.size == affixes.map { it.affixId }.toSet().size) {
            "ItemInstance cannot contain duplicate substat IDs: $instanceId"
        }
        if (mainStat != null) {
            require(mainStat.affixId !in affixes.map { it.affixId }) {
                "ItemInstance main stat cannot duplicate a substat: $instanceId"
            }
        }
        require(enhancementLevel in EnhancementLevel.INITIAL..EnhancementLevel.MAX) {
            "ItemInstance enhancement level is invalid: $enhancementLevel"
        }
    }
}
