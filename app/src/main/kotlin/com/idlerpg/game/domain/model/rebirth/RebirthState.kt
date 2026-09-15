package com.idlerpg.game.domain.model.rebirth

/**
 * Chronicle-independent Rebirth progression.
 *
 * Earned points are never removed by a later Rebirth. Allocations are permanent until the
 * player explicitly buys a gem-backed respec for one pool.
 */
data class RebirthState(
    val completedRebirths: Long = 0L,
    val normalPointsEarned: Long = 0L,
    val legacyPointsEarned: Long = 0L,
    val normalAllocations: Map<RebirthStat, Long> = emptyMap(),
    val legacyAllocations: Map<RebirthStat, Long> = emptyMap()
) {
    init {
        require(completedRebirths >= 0L) {
            "completedRebirths cannot be negative: $completedRebirths"
        }
        require(normalPointsEarned >= 0L) {
            "normalPointsEarned cannot be negative: $normalPointsEarned"
        }
        require(legacyPointsEarned >= 0L) {
            "legacyPointsEarned cannot be negative: $legacyPointsEarned"
        }
        require(normalAllocations.values.all { it >= 0L }) {
            "normal rebirth allocations cannot be negative"
        }
        require(legacyAllocations.values.all { it >= 0L }) {
            "legacy rebirth allocations cannot be negative"
        }
        require(allocatedTotal(normalAllocations) <= normalPointsEarned) {
            "normal allocations cannot exceed earned normal points"
        }
        require(allocatedTotal(legacyAllocations) <= legacyPointsEarned) {
            "legacy allocations cannot exceed earned legacy points"
        }
    }

    fun earnedPoints(pool: RebirthPointPool): Long = when (pool) {
        RebirthPointPool.NORMAL -> normalPointsEarned
        RebirthPointPool.LEGACY -> legacyPointsEarned
    }

    fun allocatedPoints(pool: RebirthPointPool): Long = when (pool) {
        RebirthPointPool.NORMAL -> allocatedTotal(normalAllocations)
        RebirthPointPool.LEGACY -> allocatedTotal(legacyAllocations)
    }

    fun unspentPoints(pool: RebirthPointPool): Long =
        earnedPoints(pool) - allocatedPoints(pool)

    fun allocation(pool: RebirthPointPool, stat: RebirthStat): Long = when (pool) {
        RebirthPointPool.NORMAL -> normalAllocations[stat] ?: 0L
        RebirthPointPool.LEGACY -> legacyAllocations[stat] ?: 0L
    }

    fun allocate(
        pool: RebirthPointPool,
        stat: RebirthStat,
        amount: Long
    ): RebirthState {
        require(amount > 0L) { "allocation amount must be positive: $amount" }
        require(amount <= unspentPoints(pool)) {
            "allocation amount exceeds unspent points: $amount"
        }
        return when (pool) {
            RebirthPointPool.NORMAL -> copy(
                normalAllocations = normalAllocations.withAdded(stat, amount)
            )
            RebirthPointPool.LEGACY -> copy(
                legacyAllocations = legacyAllocations.withAdded(stat, amount)
            )
        }
    }

    fun resetAllocations(pool: RebirthPointPool): RebirthState = when (pool) {
        RebirthPointPool.NORMAL -> copy(normalAllocations = emptyMap())
        RebirthPointPool.LEGACY -> copy(legacyAllocations = emptyMap())
    }

    private fun Map<RebirthStat, Long>.withAdded(
        stat: RebirthStat,
        amount: Long
    ): Map<RebirthStat, Long> =
        (this + (stat to Math.addExact(this[stat] ?: 0L, amount))).toMap()

    private companion object {
        fun allocatedTotal(allocations: Map<RebirthStat, Long>): Long =
            allocations.values.fold(0L) { total, points ->
                Math.addExact(total, points)
            }
    }
}
