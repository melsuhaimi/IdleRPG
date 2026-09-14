package com.idlerpg.game.domain.system.progression

import com.idlerpg.game.core.id.ContentId
import com.idlerpg.game.core.number.GameMath
import com.idlerpg.game.core.number.GameNumber
import com.idlerpg.game.data.content.ContentRegistry
import com.idlerpg.game.domain.definition.progression.LevelCurveDefinition
import com.idlerpg.game.domain.event.ExperienceGranted
import com.idlerpg.game.domain.event.GameEvent
import com.idlerpg.game.domain.event.PlayerLeveledUp
import com.idlerpg.game.domain.model.GameState
import java.math.BigInteger

/** State/events produced by one deterministic progression transition. */
data class ProgressionTransitionResult(
    val state: GameState,
    val events: List<GameEvent>
)

/** Owns exact player XP accumulation and deterministic multi-level advancement. */
object PlayerProgressionSystem {

    fun experienceToNextLevel(
        state: GameState,
        contentRegistry: ContentRegistry
    ): GameNumber {
        val level = state.run.progression.playerLevel.level
        val curve = contentRegistry.defaultPlayerLevelCurve()
        if (curve.maxLevel != null && level >= curve.maxLevel!!) {
            return GameNumber.ZERO
        }
        return experienceCostForLevel(level, curve)
    }

    fun grantExperience(
        state: GameState,
        amount: GameNumber,
        sourceId: ContentId?,
        contentRegistry: ContentRegistry
    ): ProgressionTransitionResult {
        if (amount == GameNumber.ZERO) {
            return ProgressionTransitionResult(state, emptyList())
        }

        val curve = contentRegistry.defaultPlayerLevelCurve()
        val current = state.run.progression.playerLevel
        val events = mutableListOf<GameEvent>()
        events += ExperienceGranted(amount = amount, sourceId = sourceId)

        val maxGains = maximumLevelGains(current.level, curve.maxLevel)
        if (maxGains == 0L) {
            return ProgressionTransitionResult(state, events)
        }

        val available = current.currentExperience + amount
        val gains = maximumAffordableLevelGains(
            currentLevel = current.level,
            availableExperience = available,
            curve = curve,
            maximumGains = maxGains
        )
        val consumed = cumulativeExperienceCost(
            currentLevel = current.level,
            levelGains = gains,
            curve = curve
        )
        val newLevel = Math.addExact(current.level, gains)
        val reachedMaximum = curve.maxLevel != null && newLevel >= curve.maxLevel!!
        val remaining = if (reachedMaximum) {
            GameNumber.ZERO
        } else {
            available - consumed
        }

        var transitioned = state.copy(
            run = state.run.copy(
                progression = state.run.progression.copy(
                    playerLevel = current.copy(
                        level = newLevel,
                        currentExperience = remaining
                    )
                )
            )
        )

        if (gains > 0L) {
            events += PlayerLeveledUp(
                previousLevel = current.level,
                newLevel = newLevel
            )
        }

        val unlocks = FeatureUnlockSystem.unlockEligible(
            state = transitioned,
            contentRegistry = contentRegistry
        )
        transitioned = unlocks.state
        events += unlocks.events

        return ProgressionTransitionResult(transitioned, events)
    }

    internal fun experienceCostForLevel(
        level: Long,
        curve: LevelCurveDefinition
    ): GameNumber = when (curve) {
        is LevelCurveDefinition.Linear -> {
            require(level > 0L) { "level must be positive: $level" }
                curve.baseExperienceToNextLevel +
                (curve.experienceIncrementPerLevel * (level - 1L))
        }
        is LevelCurveDefinition.Progressive -> {
            require(level > 0L) { "level must be positive: $level" }
            val offset = (level - curve.accelerationStartLevel).coerceAtLeast(0L)
            val accelerated = curve.accelerationPerLevel.toBigInteger()
                .multiply(GameMath.triangular(offset))
            GameNumber.fromBigInteger(
                curve.baseExperienceToNextLevel.toBigInteger()
                    .add(curve.experienceIncrementPerLevel.toBigInteger()
                        .multiply(BigInteger.valueOf(level - 1L)))
                    .add(accelerated)
            )
        }
    }

    internal fun cumulativeExperienceCost(
        currentLevel: Long,
        levelGains: Long,
        curve: LevelCurveDefinition
    ): GameNumber {
        require(currentLevel > 0L) { "currentLevel must be positive: $currentLevel" }
        require(levelGains >= 0L) { "levelGains cannot be negative: $levelGains" }
        if (levelGains == 0L) return GameNumber.ZERO

        return when (curve) {
            is LevelCurveDefinition.Linear -> {
                val n = BigInteger.valueOf(levelGains)
                val levelOffset = BigInteger.valueOf(currentLevel - 1L)
                val triangularSpan = n.multiply(
                    levelOffset.shiftLeft(1).add(n.subtract(BigInteger.ONE))
                ).divide(BigInteger.TWO)

                val basePart = curve.baseExperienceToNextLevel.toBigInteger().multiply(n)
                val incrementPart = curve.experienceIncrementPerLevel.toBigInteger()
                    .multiply(triangularSpan)
                GameNumber.fromBigInteger(basePart.add(incrementPart))
            }
            is LevelCurveDefinition.Progressive -> {
                val n = BigInteger.valueOf(levelGains)
                val firstLinearOffset = BigInteger.valueOf(currentLevel - 1L)
                val linearSpan = n.multiply(
                    firstLinearOffset.shiftLeft(1).add(n.subtract(BigInteger.ONE))
                ).divide(BigInteger.TWO)
                val basePart = curve.baseExperienceToNextLevel.toBigInteger().multiply(n)
                val incrementPart = curve.experienceIncrementPerLevel.toBigInteger()
                    .multiply(linearSpan)

                val endExclusive = currentLevel + levelGains
                val firstAcceleratedLevel = maxOf(currentLevel, curve.accelerationStartLevel)
                val acceleratedCount = (endExclusive - firstAcceleratedLevel).coerceAtLeast(0L)
                val firstOffset = firstAcceleratedLevel - curve.accelerationStartLevel
                val acceleratedSpan = sumTriangularRange(firstOffset, acceleratedCount)
                val acceleratedPart = curve.accelerationPerLevel.toBigInteger()
                    .multiply(acceleratedSpan)

                GameNumber.fromBigInteger(
                    basePart.add(incrementPart).add(acceleratedPart)
                )
            }
        }
    }

    /** Sum triangular(n) for n in [first, first + count), without a level-by-level loop. */
    private fun sumTriangularRange(first: Long, count: Long): BigInteger {
        if (count == 0L) return BigInteger.ZERO
        require(first >= 0L) { "first triangular offset cannot be negative" }
        val start = BigInteger.valueOf(first)
        val end = start.add(BigInteger.valueOf(count)).subtract(BigInteger.ONE)
        fun sumNaturalThrough(value: BigInteger): BigInteger =
            if (value.signum() < 0) BigInteger.ZERO
            else value.multiply(value.add(BigInteger.ONE)).divide(BigInteger.TWO)
        fun sumSquaresThrough(value: BigInteger): BigInteger =
            if (value.signum() < 0) BigInteger.ZERO
            else value.multiply(value.add(BigInteger.ONE))
                .multiply(value.shiftLeft(1).add(BigInteger.ONE))
                .divide(BigInteger.valueOf(6L))
        val sumNatural = sumNaturalThrough(end).subtract(sumNaturalThrough(start.subtract(BigInteger.ONE)))
        val sumSquares = sumSquaresThrough(end).subtract(sumSquaresThrough(start.subtract(BigInteger.ONE)))
        return sumSquares.subtract(sumNatural).divide(BigInteger.TWO)
    }

    private fun maximumLevelGains(currentLevel: Long, maxLevel: Long?): Long =
        if (maxLevel == null) {
            Long.MAX_VALUE - currentLevel
        } else {
            (maxLevel - currentLevel).coerceAtLeast(0L)
        }

    private fun maximumAffordableLevelGains(
        currentLevel: Long,
        availableExperience: GameNumber,
        curve: LevelCurveDefinition,
        maximumGains: Long
    ): Long {
        var low = 0L
        var high = maximumGains
        while (low < high) {
            val delta = high - low
            val mid = low + (delta / 2L) + (delta % 2L)
            val cost = cumulativeExperienceCost(currentLevel, mid, curve)
            if (cost <= availableExperience) {
                low = mid
            } else {
                high = mid - 1L
            }
        }
        return low
    }
}
