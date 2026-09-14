package com.idlerpg.game.domain.model.player

import com.idlerpg.game.core.number.GameNumber
import com.idlerpg.game.core.number.Ratio

/**
 * Canonical raw player stats before modifiers are applied.
 *
 * Foundation 12 activates the previously neutral health/armor/status/equipment combat
 * vocabulary. The verified prototype attack baseline remains 10.
 */
data class BaseStats(
    val attackPower: GameNumber = GameNumber.of(10L),
    val maxHealth: GameNumber = GameNumber.of(100L),
    val armor: GameNumber = GameNumber.ZERO,
    val actionSpeed: Ratio = Ratio.ONE,
    val criticalChance: Ratio = Ratio.ZERO,
    val criticalMultiplier: Ratio = Ratio.ONE,
    val effectPower: Ratio = Ratio.ONE,
    val healingPower: Ratio = Ratio.ONE
)
