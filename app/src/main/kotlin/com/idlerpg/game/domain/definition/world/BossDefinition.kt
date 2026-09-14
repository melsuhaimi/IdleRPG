package com.idlerpg.game.domain.definition.world

import com.idlerpg.game.core.id.ContentId

/**
 * Static boss progression contract.
 *
 * Foundation 7 provides boss infrastructure but the first Training Hollow content does
 * not author a boss. Boss combat remains the same encounter/combat machinery when content
 * eventually supplies a BOSS encounter.
 */
data class BossDefinition(
    val id: ContentId,
    val regionId: ContentId,
    val encounterDefinitionId: ContentId,
    val requiredNormalClears: Long = 0L,
    val chronicleMilestoneRelevant: Boolean = false
) {
    init {
        require(requiredNormalClears >= 0L) {
            "BossDefinition.requiredNormalClears cannot be negative for $id"
        }
    }
}
