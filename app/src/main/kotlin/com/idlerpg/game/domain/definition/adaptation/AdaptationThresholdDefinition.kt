package com.idlerpg.game.domain.definition.adaptation

import com.idlerpg.game.core.id.ContentId
import com.idlerpg.game.core.number.GameNumber

/**
 * Data-driven regional Adaptation pressure thresholds.
 *
 * Tier 0 is implicit below the first authored threshold. Positive tiers are authored as
 * an ordered map from tier -> minimum accumulated pressure.
 */
data class AdaptationThresholdDefinition(
    val id: ContentId,
    val minimumPressureByTier: Map<Int, GameNumber>
) {
    init {
        require(minimumPressureByTier.isNotEmpty()) {
            "AdaptationThresholdDefinition.minimumPressureByTier cannot be empty for $id"
        }
        require(minimumPressureByTier.keys.all { it > 0 }) {
            "Adaptation tiers must be positive; tier 0 is implicit for $id"
        }
        require(minimumPressureByTier.values.all { it > GameNumber.ZERO }) {
            "Adaptation thresholds must be greater than zero for $id"
        }

        val ordered = minimumPressureByTier.entries.sortedBy { it.key }
        require(ordered.map { it.key } == (1..ordered.size).toList()) {
            "Adaptation tiers must be contiguous from 1 for $id"
        }

        var previous = GameNumber.ZERO
        for ((tier, threshold) in ordered) {
            require(threshold > previous) {
                "Adaptation threshold for tier $tier must be greater than the previous tier for $id"
            }
            previous = threshold
        }
    }

    fun tierForPressure(pressure: GameNumber): Int {
        var low = 0
        var high = minimumPressureByTier.size
        while (low < high) {
            val middle = low + (high - low + 1) / 2
            if (minimumPressureByTier.getValue(middle) <= pressure) low = middle
            else high = middle - 1
        }
        return low
    }
}
