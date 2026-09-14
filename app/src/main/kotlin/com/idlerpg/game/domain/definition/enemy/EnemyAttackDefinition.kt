package com.idlerpg.game.domain.definition.enemy

import com.idlerpg.game.core.id.ContentId
import com.idlerpg.game.core.number.GameNumber
import com.idlerpg.game.core.time.GameDuration
import com.idlerpg.game.domain.definition.Affinity
import com.idlerpg.game.domain.definition.DamageKind

/**
 * Authored deterministic enemy attack profile.
 *
 * Enemy attacks are static content. Runtime deadlines remain canonical in CombatState so
 * active/offline simulation, save/load, and deterministic replay use the same cadence.
 */
data class EnemyAttackDefinition(
    val id: ContentId,
    val displayName: String,
    val interval: GameDuration,
    val baseDamage: GameNumber,
    val armorPenetration: GameNumber = GameNumber.ZERO,
    val affinity: Affinity,
    val damageKind: DamageKind = DamageKind.PHYSICAL,
    val appliedStatusId: ContentId? = null,
    val resonanceDrain: GameNumber = GameNumber.ZERO
) {
    init {
        require(displayName.isNotBlank()) { "EnemyAttackDefinition.displayName cannot be blank for $id" }
        require(interval > GameDuration.ZERO) { "EnemyAttackDefinition.interval must be > 0 for $id" }
        require(baseDamage > GameNumber.ZERO) { "EnemyAttackDefinition.baseDamage must be > 0 for $id" }
        require(resonanceDrain >= GameNumber.ZERO) {
            "EnemyAttackDefinition.resonanceDrain cannot be negative for $id"
        }
    }
}
