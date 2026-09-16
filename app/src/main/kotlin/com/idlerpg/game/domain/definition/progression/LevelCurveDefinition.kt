package com.idlerpg.game.domain.definition.progression

import com.idlerpg.game.core.id.ContentId
import com.idlerpg.game.core.number.GameNumber
import com.idlerpg.game.core.number.Ratio

/**
 * Authored player-level progression curve.
 *
 * Curve shapes are explicit data contracts. This keeps the formula tunable in content while
 * leaving the authoritative arithmetic in PlayerProgressionSystem.
 */
sealed interface LevelCurveDefinition {
    val id: ContentId
    val maxLevel: Long?

    data class Linear(
        override val id: ContentId,
        val baseExperienceToNextLevel: GameNumber,
        val experienceIncrementPerLevel: GameNumber,
        override val maxLevel: Long? = null
    ) : LevelCurveDefinition {
        init {
            require(baseExperienceToNextLevel > GameNumber.ZERO) {
                "Level curve base experience must be > 0 for $id"
            }
            require(maxLevel == null || maxLevel >= 1L) {
                "Level curve maxLevel must be >= 1 when present for $id"
            }
        }
    }

    /**
     * Linear early game with a gentle quadratic acceleration after [accelerationStartLevel].
     *
     * Cost at level L is:
     * `base + increment * (L - 1) + acceleration * triangular(max(L - start, 0))`.
     * The early levels remain easy to read, while long-run requirements do not stay flat.
     */
    data class Progressive(
        override val id: ContentId,
        val baseExperienceToNextLevel: GameNumber,
        val experienceIncrementPerLevel: GameNumber,
        val accelerationStartLevel: Long = 10L,
        val accelerationPerLevel: GameNumber = GameNumber.ZERO,
        /** Optional exact compounding multiplier for the early exponential segment. */
        val compoundingMultiplierPerLevel: Ratio? = null,
        /** Inclusive level at which the early exponential segment ends. */
        val softCapLevel: Long? = null,
        /** Optional exact compounding multiplier for the post-soft-cap segment. */
        val postSoftCapCompoundingMultiplierPerLevel: Ratio? = null,
        override val maxLevel: Long? = null
    ) : LevelCurveDefinition {
        init {
            require(baseExperienceToNextLevel > GameNumber.ZERO) {
                "Level curve base experience must be > 0 for $id"
            }
            require(experienceIncrementPerLevel >= GameNumber.ZERO) {
                "Level curve experience increment cannot be negative for $id"
            }
            require(accelerationStartLevel >= 1L) {
                "Level curve accelerationStartLevel must be >= 1 for $id"
            }
            require(accelerationPerLevel >= GameNumber.ZERO) {
                "Level curve acceleration cannot be negative for $id"
            }
            require(maxLevel == null || maxLevel >= 1L) {
                "Level curve maxLevel must be >= 1 when present for $id"
            }

            if (compoundingMultiplierPerLevel == null) {
                require(softCapLevel == null) {
                    "Level curve softCapLevel requires compounding progression for $id"
                }
                require(postSoftCapCompoundingMultiplierPerLevel == null) {
                    "Level curve post-soft-cap multiplier requires compounding progression for $id"
                }
            } else {
                require(compoundingMultiplierPerLevel >= Ratio.ONE) {
                    "Level curve compounding multiplier must be >= 1 for $id"
                }
                require(softCapLevel != null && softCapLevel >= 1L) {
                    "Level curve softCapLevel must be >= 1 for $id"
                }
                require(postSoftCapCompoundingMultiplierPerLevel != null) {
                    "Level curve post-soft-cap multiplier is required for $id"
                }
                require(postSoftCapCompoundingMultiplierPerLevel >= Ratio.ONE) {
                    "Level curve post-soft-cap multiplier must be >= 1 for $id"
                }
            }
        }
    }
}
