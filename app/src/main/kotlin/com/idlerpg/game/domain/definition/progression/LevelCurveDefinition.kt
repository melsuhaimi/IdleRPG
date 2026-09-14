package com.idlerpg.game.domain.definition.progression

import com.idlerpg.game.core.id.ContentId
import com.idlerpg.game.core.number.GameNumber

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
        }
    }
}
