package com.idlerpg.game.domain.engine

import com.idlerpg.game.core.id.ContentId
import com.idlerpg.game.core.id.InstanceId
import com.idlerpg.game.core.time.GameTime

/**
 * One deterministic autonomous action projected from canonical state.
 *
 * Ordering is defined only by:
 * 1. [dueAt],
 * 2. [priority],
 * 3. [stableTieBreakKey].
 *
 * A scheduled action is not itself canonical mutable state. Later systems derive actions
 * from canonical state such as cooldown deadlines, encounter transitions, and decision
 * timestamps. At least one stable owner/source reference is required so the handler can
 * resolve the action back to canonical state/content without carrying mutable objects.
 */
data class ScheduledAction(
    val dueAt: GameTime,
    val type: ScheduledActionType,
    val stableTieBreakKey: Long,
    val ownerInstanceId: InstanceId? = null,
    val sourceContentId: ContentId? = null,
    val priority: EnginePriority = type.defaultPriority
) {
    init {
        require(stableTieBreakKey >= 0L) {
            "ScheduledAction.stableTieBreakKey cannot be negative: $stableTieBreakKey"
        }
        require(ownerInstanceId != null || sourceContentId != null) {
            "ScheduledAction requires an ownerInstanceId or sourceContentId"
        }
    }
}
