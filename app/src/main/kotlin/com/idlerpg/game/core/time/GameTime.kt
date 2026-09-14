package com.idlerpg.game.core.time

/**
 * Deterministic in-game simulation timestamp.
 *
 * Simulation time starts at [ZERO] and only advances through the simulation engine.
 * It is deliberately separate from wall-clock epoch time.
 */
class GameTime private constructor(
    val millis: Long
) : Comparable<GameTime> {

    init {
        require(millis >= 0L) {
            "GameTime cannot be negative: $millis ms"
        }
    }

    operator fun plus(duration: GameDuration): GameTime =
        ofMillis(Math.addExact(millis, duration.millis))

    operator fun minus(duration: GameDuration): GameTime {
        require(millis >= duration.millis) {
            "GameTime subtraction would precede simulation origin: $millis - ${duration.millis}"
        }
        return ofMillis(millis - duration.millis)
    }

    operator fun minus(other: GameTime): GameDuration {
        require(millis >= other.millis) {
            "Cannot produce a negative GameDuration from $millis - ${other.millis}"
        }
        return GameDuration.ofMillis(millis - other.millis)
    }

    override fun compareTo(other: GameTime): Int = millis.compareTo(other.millis)

    override fun equals(other: Any?): Boolean =
        other is GameTime && millis == other.millis

    override fun hashCode(): Int = millis.hashCode()

    override fun toString(): String = "GameTime(${millis}ms)"

    companion object {
        val ZERO: GameTime = GameTime(0L)

        fun ofMillis(millis: Long): GameTime = GameTime(millis)
    }
}
