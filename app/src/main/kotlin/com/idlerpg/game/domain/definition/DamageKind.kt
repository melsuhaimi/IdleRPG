package com.idlerpg.game.domain.definition

import com.idlerpg.game.core.id.ContentId

/**
 * Canonical damage-kind vocabulary.
 *
 * Foundation 5 requires only the prototype's physical basic attack. Additional damage
 * kinds must be added only when later combat content explicitly requires them.
 */
enum class DamageKind(
    val id: ContentId
) {
    PHYSICAL(ContentId("damage.physical")),
    ELEMENTAL(ContentId("damage.elemental")),
    ARCANE(ContentId("damage.arcane")),
    SHADOW(ContentId("damage.shadow"))
}
