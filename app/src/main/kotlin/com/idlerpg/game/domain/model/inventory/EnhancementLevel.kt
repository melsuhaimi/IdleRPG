package com.idlerpg.game.domain.model.inventory

/** BDO-style enhancement ladder represented by one stable persisted integer. */
object EnhancementLevel {
    const val INITIAL: Int = 0
    const val MAX_PLUS_LEVEL: Int = 15
    const val PRI: Int = 16
    const val DUO: Int = 17
    const val TRI: Int = 18
    const val TET: Int = 19
    const val PEN: Int = 20
    const val MAX: Int = PEN

    /**
     * Failstacks are a bounded additive chance bonus, not a pity or guarantee.
     * Twenty failures can add at most five percentage points to the authored chance.
     */
    const val MAX_FAILSTACK: Int = 20
    const val FAILSTACK_BONUS_UNITS_PER_STACK: Long = 25L
    const val MAX_FAILSTACK_BONUS_UNITS: Long = 500L

    fun isHighRisk(level: Int): Boolean = level >= PRI

    fun nextFailstack(value: Int): Int {
        require(value in 0..MAX_FAILSTACK) {
            "Failstack is invalid: $value"
        }
        return (value + 1).coerceAtMost(MAX_FAILSTACK)
    }

    fun failstackBonusUnits(value: Int): Long {
        require(value in 0..MAX_FAILSTACK) {
            "Failstack is invalid: $value"
        }
        return minOf(
            value.toLong() * FAILSTACK_BONUS_UNITS_PER_STACK,
            MAX_FAILSTACK_BONUS_UNITS
        )
    }

    fun displayName(level: Int): String {
        require(level in INITIAL..MAX) { "Unknown enhancement level: $level" }
        return when {
            level == INITIAL -> "+0"
            level <= MAX_PLUS_LEVEL -> "+$level"
            level == PRI -> "PRI"
            level == DUO -> "DUO"
            level == TRI -> "TRI"
            level == TET -> "TET"
            else -> "PEN"
        }
    }

    fun next(level: Int): Int? =
        if (level >= MAX) null else level + 1

    fun downgradeAfterFailure(level: Int): Int =
        if (isHighRisk(level)) (level - 1).coerceAtLeast(INITIAL) else level
}
