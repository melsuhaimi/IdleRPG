package com.idlerpg.game.domain.model.combat

import com.idlerpg.game.core.id.ContentId
import com.idlerpg.game.core.time.GameTime

/** Skill/action readiness deadlines expressed only in deterministic simulation time. */
data class CooldownState(
    val readyAtByActionId: Map<ContentId, GameTime> = emptyMap()
)
