package com.idlerpg.game.core.config

import com.idlerpg.game.core.number.GameNumber
import com.idlerpg.game.core.number.Ratio

/** Bounded long-run ecology. Early thresholds and mutation strengths remain unchanged. */
object AdaptationCurve {
    const val MAXIMUM_TIER = 1_000

    fun threshold(tier: Int): GameNumber {
        require(tier in 1..MAXIMUM_TIER)
        val depth = (tier - 4).toLong()
        return GameNumber.of(when (tier) {
            1 -> 60L
            2 -> 250L
            3 -> 500L
            4 -> 900L
            else -> 900L + 400L * depth + 25L * depth * depth
        })
    }

    // At tier 1000, matching affinity damage receives at most 25% additional reduction.
    fun resistanceMultiplier(tier: Int): Ratio = Ratio.ofUnits(
        Ratio.ONE.units - depth(tier) * 2_500L / 996L
    )

    // Rewards rise alongside resistance: at most +50 percentage points per carrier.
    fun rewardBonusUnits(tier: Int): Long = depth(tier) * 5_000L / 996L

    private fun depth(tier: Int): Long = (tier.coerceIn(1, MAXIMUM_TIER) - 4).coerceAtLeast(0).toLong()
}
