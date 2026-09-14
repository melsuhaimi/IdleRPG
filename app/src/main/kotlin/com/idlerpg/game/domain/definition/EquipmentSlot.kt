package com.idlerpg.game.domain.definition

import com.idlerpg.game.core.id.ContentId

/**
 * Fixed equipment-slot vocabulary.
 *
 * The slot identity is stable and is not a localized display string.
 */
enum class EquipmentSlot(
    val id: ContentId
) {
    WEAPON(ContentId("slot.weapon")),
    ARMOR(ContentId("slot.armor")),
    HELM(ContentId("slot.helm")),
    BOOTS(ContentId("slot.boots")),
    ACCESSORY(ContentId("slot.accessory")),
    CATALYST(ContentId("slot.catalyst"));

    companion object {
        fun fromId(id: ContentId): EquipmentSlot? =
            values().firstOrNull { it.id == id }
    }
}
