package com.idlerpg.game.domain.model.combat

import com.idlerpg.game.core.id.InstanceId
import com.idlerpg.game.core.time.GameTime

/** High-level lifecycle of the current encounter's combat execution. */
enum class CombatStatus {
    IDLE,
    ACTIVE,
    VICTORY,
    DEFEAT
}

/**
 * Current encounter combat state.
 *
 * Baseline Foundation 2 state is IDLE with no instantiated combatants. Combat creation
 * and scheduling are implemented later rather than hidden in this data model.
 */
data class CombatState(
    val status: CombatStatus = CombatStatus.IDLE,
    val playerCombatant: CombatantState? = null,
    val enemies: List<EnemyState> = emptyList(),
    val nextPlayerDecisionAt: GameTime? = null,
    val nextEnemyDecisionAt: Map<InstanceId, GameTime> = emptyMap(),
    val combatSequenceId: Long = 0L,
    val encounterStartedAt: GameTime? = null,
    val queuedPlayerAction: QueuedPlayerAction? = null
) {
    init {
        require(combatSequenceId >= 0L) {
            "combatSequenceId cannot be negative: $combatSequenceId"
        }
        require(enemies.map { it.instanceId }.size == enemies.map { it.instanceId }.toSet().size) {
            "enemies must have unique instance IDs"
        }
        require(nextEnemyDecisionAt.keys.all { id -> enemies.any { it.instanceId == id } }) {
            "nextEnemyDecisionAt cannot reference an enemy absent from enemies"
        }
        if (status == CombatStatus.IDLE) {
            require(enemies.isEmpty()) { "IDLE combat cannot contain enemies" }
            require(nextPlayerDecisionAt == null) { "IDLE combat cannot schedule player decisions" }
            require(nextEnemyDecisionAt.isEmpty()) { "IDLE combat cannot schedule enemy decisions" }
            require(encounterStartedAt == null) { "IDLE combat cannot have encounterStartedAt" }
            require(queuedPlayerAction == null) { "IDLE combat cannot retain queued player action" }
        }
    }
}
