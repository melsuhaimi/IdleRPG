package com.idlerpg.game.core.time

/**
 * Non-negative elapsed deterministic simulation duration.
 *
 * This type is platform-neutral. It does not read wall-clock time and cannot represent
 * a negative interval. Arithmetic uses exact Long operations so overflow fails fast.
 */
class GameDuration private constructor(
    val millis: Long
) : Comparable<GameDuration> {

    init {
        require(millis >= 0L) {
            "GameDuration cannot be negative: $millis ms"
        }
    }

    operator fun plus(other: GameDuration): GameDuration =
        ofMillis(Math.addExact(millis, other.millis))

    operator fun minus(other: GameDuration): GameDuration {
        require(millis >= other.millis) {
            "GameDuration subtraction would be negative: $millis - ${other.millis}"
        }
        return ofMillis(millis - other.millis)
    }

    override fun compareTo(other: GameDuration): Int =
        millis.compareTo(other.millis)

    override fun equals(other: Any?): Boolean =
        other is GameDuration && millis == other.millis

    override fun hashCode(): Int = millis.hashCode()

    override fun toString(): String = "${millis}ms"

    companion object {
        val ZERO: GameDuration = GameDuration(0L)

        fun ofMillis(millis: Long): GameDuration = GameDuration(millis)

        fun ofSeconds(seconds: Long): GameDuration {
            require(seconds >= 0L) { "seconds cannot be negative: $seconds" }
            return ofMillis(Math.multiplyExact(seconds, 1_000L))
        }

        fun ofMinutes(minutes: Long): GameDuration {
            require(minutes >= 0L) { "minutes cannot be negative: $minutes" }
            return ofSeconds(Math.multiplyExact(minutes, 60L))
        }

        fun ofHours(hours: Long): GameDuration {
            require(hours >= 0L) { "hours cannot be negative: $hours" }
            return ofMinutes(Math.multiplyExact(hours, 60L))
        }
    }
}
