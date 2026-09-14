package com.idlerpg.game.domain.model.combat

import com.idlerpg.game.core.id.ContentId

/**
 * Canonical manual player intent waiting for a normal deterministic player-decision slot.
 *
 * FBE-00 adds persistence/state ownership only. FBE-01 will add commands and execution
 * semantics. Keeping this domain-only type free of UI concepts preserves the canonical
 * GameState boundary.
 */
sealed interface QueuedPlayerAction {
    data class Skill(
        val skillId: ContentId
    ) : QueuedPlayerAction
}
