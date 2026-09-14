package com.idlerpg.game.domain.system.enemy

import com.idlerpg.game.core.id.ContentId
import com.idlerpg.game.domain.definition.enemy.EnemyDefinition

/**
 * Deterministic enemy action-priority boundary.
 *
 * Skill execution itself is not implemented until the skill/status foundation. When an
 * enemy has authored skills, this policy exposes the stable first action by ContentId
 * rather than depending on collection insertion order. The Foundation 7 Slime has no
 * skills and therefore selects no enemy action.
 */
object EnemyBehaviorSystem {

    fun selectAttackId(enemyDefinition: EnemyDefinition): ContentId? =
        enemyDefinition.attackDefinitionId

    fun selectActionId(
        enemyDefinition: EnemyDefinition
    ): ContentId? =
        enemyDefinition.skillIds
            .sorted()
            .firstOrNull()
}
