package com.idlerpg.game.domain.model.combat

import com.idlerpg.game.core.id.ContentId
import com.idlerpg.game.core.id.InstanceId
import com.idlerpg.game.domain.model.adaptation.ActiveMutationState

/**
 * Runtime enemy instance.
 *
 * Static name, base stats, authored loot, and other definition data are intentionally not
 * duplicated here. Adaptation mutations are snapshotted at spawn so regional pressure
 * changes cannot retroactively alter an already-active enemy.
 */
data class EnemyState(
    val instanceId: InstanceId,
    val definitionId: ContentId,
    val combatant: CombatantState,
    val scalingTier: Long = 0L,
    val activeTraitIds: Set<ContentId> = emptySet(),
    val activeMutations: List<ActiveMutationState> = emptyList()
) {
    val activeMutationIds: Set<ContentId>
        get() = activeMutations.map { it.mutationId }.toSet()

    init {
        require(combatant.instanceId == instanceId) {
            "EnemyState.instanceId must match combatant.instanceId"
        }
        require(scalingTier >= 0L) { "scalingTier cannot be negative: $scalingTier" }
        require(activeMutations.map { it.mutationId }.size ==
            activeMutations.map { it.mutationId }.toSet().size
        ) {
            "EnemyState.activeMutations cannot contain duplicate mutation IDs"
        }
    }
}
