package com.idlerpg.game.domain.engine

/**
 * Canonical deterministic priority categories for simultaneous scheduled actions.
 *
 * Lower [order] values execute first. The numeric values are an engine compatibility
 * contract: content/systems must not rely on enum declaration order.
 */
enum class EnginePriority(
    val order: Int
) {
    ENCOUNTER_LIFECYCLE(10),
    STATUS_PERIODIC(20),
    PLAYER_DECISION(30),
    ENEMY_DECISION(40),
    DEFERRED_SYSTEM_ACTION(50);

    companion object {
        /** Foundation 4 supports scheduler ordering policy version 1 only. */
        const val ORDERING_VERSION: Int = 1
    }
}
