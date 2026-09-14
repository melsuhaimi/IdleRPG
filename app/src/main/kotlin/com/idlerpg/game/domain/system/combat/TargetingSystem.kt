package com.idlerpg.game.domain.system.combat

import com.idlerpg.game.core.number.GameNumber
import com.idlerpg.game.domain.model.combat.CombatState
import com.idlerpg.game.domain.model.combat.EnemyState
import com.idlerpg.game.data.content.ContentRegistry
import com.idlerpg.game.domain.definition.enemy.EnemyRole

/** Reusable deterministic enemy selection. Stable InstanceId ordering breaks all ties. */
object TargetingSystem {
    fun allLivingEnemies(combat: CombatState): List<EnemyState> =
        combat.enemies.filter { it.combatant.currentHealth > GameNumber.ZERO }
            .sortedBy { it.instanceId }

    fun primaryLivingEnemy(combat: CombatState): EnemyState? =
        allLivingEnemies(combat).firstOrNull()

    fun lowestHealthEnemy(combat: CombatState): EnemyState? =
        allLivingEnemies(combat).minWithOrNull(
            compareBy<EnemyState> { it.combatant.currentHealth }.thenBy { it.instanceId }
        )

    fun highestHealthEnemy(combat: CombatState): EnemyState? =
        allLivingEnemies(combat).maxWithOrNull(
            compareBy<EnemyState> { it.combatant.currentHealth }.thenByDescending { it.instanceId }
        )

    fun firstWithRole(
        combat: CombatState,
        contentRegistry: ContentRegistry,
        roles: Set<EnemyRole>
    ): EnemyState? =
        allLivingEnemies(combat).firstOrNull { enemy ->
            contentRegistry.enemy(enemy.definitionId).role in roles
        }
}
