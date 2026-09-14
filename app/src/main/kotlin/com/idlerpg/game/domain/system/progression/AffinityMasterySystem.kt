package com.idlerpg.game.domain.system.progression

import com.idlerpg.game.core.number.GameNumber
import com.idlerpg.game.data.content.ContentRegistry
import com.idlerpg.game.domain.definition.Affinity
import com.idlerpg.game.domain.definition.progression.MasteryDefinition
import com.idlerpg.game.domain.event.GameEvent
import com.idlerpg.game.domain.event.MasteryIncreased
import com.idlerpg.game.domain.model.GameState
import java.math.BigInteger

/** Owns run-level affinity mastery earned from effective player contribution. */
object AffinityMasterySystem {

    fun grantEffectiveContribution(
        state: GameState,
        affinities: List<Affinity>,
        effectiveAmount: GameNumber,
        contentRegistry: ContentRegistry
    ): ProgressionTransitionResult {
        if (effectiveAmount == GameNumber.ZERO) {
            return ProgressionTransitionResult(state, emptyList())
        }

        val orderedAffinityOccurrences = affinities.sortedBy { it.id }
        if (orderedAffinityOccurrences.isEmpty()) {
            return ProgressionTransitionResult(state, emptyList())
        }

        val shares = splitContribution(effectiveAmount, orderedAffinityOccurrences.size)
        val contributionByAffinityId = linkedMapOf<com.idlerpg.game.core.id.ContentId, GameNumber>()
        orderedAffinityOccurrences.forEachIndexed { index, affinity ->
            if (contentRegistry.masteryForAffinityOrNull(affinity.id) == null) {
                return@forEachIndexed
            }
            val share = shares[index]
            val current = contributionByAffinityId[affinity.id] ?: GameNumber.ZERO
            contributionByAffinityId[affinity.id] = current + share
        }

        var experience = state.run.progression.affinityMastery.experienceByAffinityId
        val events = mutableListOf<GameEvent>()

        contributionByAffinityId.entries.sortedBy { it.key }.forEach { (affinityId, share) ->
            if (share == GameNumber.ZERO) return@forEach
            val current = experience[affinityId] ?: GameNumber.ZERO
            experience = experience + (affinityId to (current + share))
            events += MasteryIncreased(
                affinityId = affinityId,
                amount = share
            )
        }

        var transitioned = state.copy(
            run = state.run.copy(
                progression = state.run.progression.copy(
                    affinityMastery = state.run.progression.affinityMastery.copy(
                        experienceByAffinityId = experience
                    )
                )
            )
        )

        val unlocks = FeatureUnlockSystem.unlockEligible(
            state = transitioned,
            contentRegistry = contentRegistry
        )
        transitioned = unlocks.state
        events += unlocks.events

        return ProgressionTransitionResult(transitioned, events)
    }

    fun levelFor(
        state: GameState,
        affinityId: com.idlerpg.game.core.id.ContentId,
        contentRegistry: ContentRegistry
    ): Long {
        val definition = contentRegistry.masteryForAffinityOrNull(affinityId) ?: return 1L
        val experience = state.run.progression.affinityMastery
            .experienceByAffinityId[affinityId] ?: GameNumber.ZERO
        return levelFor(experience, definition)
    }

    fun experienceToNextLevel(
        state: GameState,
        affinityId: com.idlerpg.game.core.id.ContentId,
        contentRegistry: ContentRegistry
    ): GameNumber {
        val definition = contentRegistry.masteryForAffinityOrNull(affinityId)
            ?: return GameNumber.ZERO
        val level = levelFor(state, affinityId, contentRegistry)
        val maximumLevel = definition.maxLevel
        if (maximumLevel != null && level >= maximumLevel) {
            return GameNumber.ZERO
        }
        val total = state.run.progression.affinityMastery
            .experienceByAffinityId[affinityId] ?: GameNumber.ZERO
        if (level == Long.MAX_VALUE) return GameNumber.ZERO
        val nextThreshold = cumulativeCostFromLevelOne(level + 1L, definition)
        return if (nextThreshold > total) nextThreshold - total else GameNumber.ZERO
    }

    private fun levelFor(
        experience: GameNumber,
        definition: MasteryDefinition
    ): Long {
        val maximumLevel = definition.maxLevel
        val maximumGains = if (maximumLevel == null) {
            Long.MAX_VALUE - 1L
        } else {
            (maximumLevel - 1L).coerceAtLeast(0L)
        }

        var low = 0L
        var high = maximumGains
        while (low < high) {
            val delta = high - low
            val mid = low + (delta / 2L) + (delta % 2L)
            val cost = cumulativeCostFromLevelOne(mid + 1L, definition)
            if (cost <= experience) {
                low = mid
            } else {
                high = mid - 1L
            }
        }
        return 1L + low
    }

    /** Total mastery experience required to have reached [level]. */
    private fun cumulativeCostFromLevelOne(
        level: Long,
        definition: MasteryDefinition
    ): GameNumber {
        require(level > 0L) { "Mastery level must be positive: $level" }
        val gains = level - 1L
        if (gains == 0L) return GameNumber.ZERO

        val n = BigInteger.valueOf(gains)
        val triangular = n.multiply(n.subtract(BigInteger.ONE)).divide(BigInteger.TWO)
        val basePart = definition.baseExperienceToNextLevel.toBigInteger().multiply(n)
        val incrementPart = definition.experienceIncrementPerLevel.toBigInteger()
            .multiply(triangular)
        return GameNumber.fromBigInteger(basePart.add(incrementPart))
    }

    private fun splitContribution(
        total: GameNumber,
        parts: Int
    ): List<GameNumber> {
        require(parts > 0) { "parts must be positive" }
        val divisor = BigInteger.valueOf(parts.toLong())
        val (quotient, remainder) = total.toBigInteger().divideAndRemainder(divisor)
        val remainderCount = remainder.toInt()
        return List(parts) { index ->
            GameNumber.fromBigInteger(
                if (index < remainderCount) quotient.add(BigInteger.ONE) else quotient
            )
        }
    }
}
