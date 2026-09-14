package com.idlerpg.game.domain.engine

/**
 * Engine-level categories of autonomous scheduled work.
 *
 * The types deliberately describe scheduling semantics rather than feature formulas.
 * Combat/world/status systems introduced by later foundations will derive concrete
 * [ScheduledAction] instances from canonical GameState.
 */
enum class ScheduledActionType(
    val defaultPriority: EnginePriority
) {
    ENCOUNTER_LIFECYCLE(EnginePriority.ENCOUNTER_LIFECYCLE),
    STATUS_PERIODIC(EnginePriority.STATUS_PERIODIC),
    PLAYER_DECISION(EnginePriority.PLAYER_DECISION),
    ENEMY_DECISION(EnginePriority.ENEMY_DECISION),
    DEFERRED_SYSTEM_ACTION(EnginePriority.DEFERRED_SYSTEM_ACTION)
}
