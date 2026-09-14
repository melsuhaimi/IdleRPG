package com.idlerpg.game.domain.system.enemy

import com.idlerpg.game.core.number.GameMath
import com.idlerpg.game.core.number.GameNumber
import com.idlerpg.game.core.number.Ratio
import com.idlerpg.game.core.time.GameDuration
import com.idlerpg.game.domain.definition.world.EliteModifier
import com.idlerpg.game.domain.model.combat.EnemyState

/** Pure bounded interpretation of reusable elite modifier IDs. */
object EnemyEliteSystem {
    fun spawnHealth(base: GameNumber, traitIds: Set<com.idlerpg.game.core.id.ContentId>): GameNumber =
        if (EliteModifier.SHIELDED.id in traitIds) {
            GameMath.applyRatio(base, Ratio.ofUnits(12_500L))
        } else {
            base
        }

    fun attackInterval(base: GameDuration, enemy: EnemyState): GameDuration =
        if (EliteModifier.FRENZIED.id in enemy.activeTraitIds) {
            GameDuration.ofMillis(maxOf(250L, base.millis * 3L / 4L))
        } else {
            base
        }

    fun attackDamage(base: GameNumber, enemy: EnemyState): GameNumber =
        if (EliteModifier.RESONANT.id in enemy.activeTraitIds) {
            GameMath.applyRatio(base, Ratio.ofUnits(12_500L))
        } else {
            base
        }

    fun regenerationAmount(maximumHealth: GameNumber, enemy: EnemyState): GameNumber =
        if (EliteModifier.REGENERATING.id in enemy.activeTraitIds) {
            maximumHealth.divide(20L)
        } else {
            GameNumber.ZERO
        }
}
